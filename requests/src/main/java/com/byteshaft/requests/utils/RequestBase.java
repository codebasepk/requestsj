/*
 * Requests, an implementation of XmlHttpRequest for Android
 * Copyright (C) 2016 byteShaft
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.byteshaft.requests.utils;

import android.content.Context;

import com.byteshaft.requests.HttpRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class RequestBase {

    protected HttpURLConnection mConnection;
    protected OutputStream mOutputStream;
    protected String mResponseText = "";
    protected short mReadyState = HttpRequest.STATE_UNSET;
    protected String mUrl;
    protected HttpRequest mRequest;

    private ArrayList<HttpRequest.OnReadyStateChangeListener> mStateChangeListeners;
    private ArrayList<HttpRequest.FileUploadProgressListener> mProgressListeners;
    private ListenersUtil mListenersUtil;

    protected RequestBase(Context context) {
        mStateChangeListeners = new ArrayList<>();
        mProgressListeners = new ArrayList<>();
        mListenersUtil = ListenersUtil.getInstance(context);
    }

    protected void openConnection(String requestMethod, String url) {
        mUrl = url;
        try {
            URL urlObject = new URL(mUrl);
            mConnection = (HttpURLConnection) urlObject.openConnection();
            mConnection.setRequestMethod(requestMethod);
            mReadyState = HttpRequest.STATE_OPENED;
            mListenersUtil.emitOnReadyStateChange(mStateChangeListeners, mConnection, mReadyState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void readResponse() {
        mReadyState = HttpRequest.STATE_LOADING;
        mListenersUtil.emitOnReadyStateChange(mStateChangeListeners, mConnection, mReadyState);
        try {
            InputStream inputStream = mConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            mResponseText = output.toString();
        } catch (IOException ignore) {

        }
        mReadyState = HttpRequest.STATE_DONE;
        mListenersUtil.emitOnReadyStateChange(mStateChangeListeners, mConnection, mReadyState);
    }

    protected void sendRequestData(String body, boolean closeOnDone) {
        try {
            byte[] outputInBytes = body.getBytes("UTF-8");
            if (mOutputStream == null) {
                mOutputStream = mConnection.getOutputStream();
            }
            mOutputStream.write(outputInBytes);
            mOutputStream.flush();
            if (closeOnDone) {
                mOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (closeOnDone) {
            mReadyState = HttpRequest.STATE_HEADERS_RECEIVED;
            mListenersUtil.emitOnReadyStateChange(mStateChangeListeners, mConnection, mReadyState);
        }
    }

    protected void writeContent(String uploadFilePath) {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
            mOutputStream.flush();
            FileInputStream inputStream = new FileInputStream(uploadFile);
            final byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
                uploaded += bytesRead;
                mListenersUtil.emitOnFileUploadProgress(
                        mProgressListeners,
                        uploadFile,
                        uploaded,
                        total
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addReadyStateListener(HttpRequest.OnReadyStateChangeListener listener) {
        mStateChangeListeners.add(listener);
    }

    protected void addProgressUpdateListener(
            HttpRequest.FileUploadProgressListener listener
    ) {
        mProgressListeners.add(listener);
    }
}
