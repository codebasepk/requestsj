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
package com.byteshaft.requests;

import android.content.Context;

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

class BaseHttpRequest extends EventCentral {

    HttpURLConnection mConnection;
    int mFilesCount;
    int mCurrentFileNumber;
    private OutputStream mOutputStream;
    short mStatus;
    String mStatusText;
    String mResponseText;
    String mUrl;
    final String CONTENT_TYPE_JSON = "application/json";
    final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY
    );

    BaseHttpRequest(Context context) {
        super(context);
    }

    void openConnection(String requestMethod, String url) {
        mUrl = url;
        try {
            URL urlObject = new URL(mUrl);
            mConnection = (HttpURLConnection) urlObject.openConnection();
            mConnection.setRequestMethod(requestMethod);
            emitOnReadyStateChange(HttpRequest.STATE_OPENED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendRequest(String contentType, final String data) {
        mConnection.setRequestProperty("Content-Type", contentType);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (data != null) {
                    sendRequestData(data, true);
                }
                readResponse();
            }
        }).start();
    }

    void sendRequest(String contentType, final FormData data) {
        mConnection.setRequestProperty("Content-Type", contentType);
        mConnection.setFixedLengthStreamingMode(data.getContentLength());
        mFilesCount = data.getFilesCount();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<FormData.MultiPartData> requestItems = data.getData();
                for (FormData.MultiPartData item : requestItems) {
                    sendRequestData(item.getPreContentData(), false);
                    if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                        sendRequestData(item.getContent(), false);
                    } else {
                        mCurrentFileNumber += 1;
                        writeContent(item.getContent());
                    }
                    sendRequestData(item.getPostContentData(), false);
                }
                sendRequestData(FormData.FINISH_LINE, true);
                readResponse();
            }
        }).start();
    }

    private void readResponse() {
        emitOnReadyStateChange(HttpRequest.STATE_LOADING);
        InputStream inputStream;
        try {
            inputStream = mConnection.getInputStream();
            readFromInputStream(inputStream);
            assignResponseCodeAndMessage();
            emitOnReadyStateChange(HttpRequest.STATE_DONE);
        } catch (IOException ignore) {
            inputStream = mConnection.getErrorStream();
            readFromInputStream(inputStream);
            assignResponseCodeAndMessage();
            emitOnReadyStateChange(HttpRequest.STATE_DONE);
        }
    }

    private void assignResponseCodeAndMessage() {
        try {
            mStatus = (short) mConnection.getResponseCode();
            mStatusText = mConnection.getResponseMessage();
        } catch (IOException ignore) {

        }
    }

    private void readFromInputStream(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            mResponseText = output.toString();
        } catch (IOException ignore) {

        }
    }

    private void sendRequestData(String body, boolean closeOnDone) {
        try {
            byte[] outputInBytes = body.getBytes();
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
            emitOnReadyStateChange(HttpRequest.STATE_HEADERS_RECEIVED);
        }
    }

    private void writeContent(String uploadFilePath) {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
            mOutputStream.flush();
            FileInputStream inputStream = new FileInputStream(uploadFile);
            final byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
                uploaded += bytesRead;
                emitOnFileUploadProgress(uploadFile, uploaded, total);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
