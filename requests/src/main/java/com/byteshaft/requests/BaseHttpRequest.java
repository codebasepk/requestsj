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
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.SSLHandshakeException;

class BaseHttpRequest extends EventCentral {

    private static final String TAG = "BaseHttpRequest";
    private OutputStream mOutputStream;

    final String CONTENT_TYPE_JSON = "application/json";
    final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);
    int mFilesCount;
    int mCurrentFileNumber;
    int mConnectTimeout = 15000;
    short mStatus;
    HttpURLConnection mConnection;
    String mStatusText;
    String mResponseText;
    String mUrl;

    BaseHttpRequest(Context context) {
        super(context);
    }

    void setupConnection(String requestMethod, String url) {
        mUrl = url;
        try {
            URL urlObject = new URL(mUrl);
            mConnection = (HttpURLConnection) urlObject.openConnection();
            mConnection.setRequestMethod(requestMethod);
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                emitOnError(HttpRequest.ERROR_INVALID_URL, e);
            } else if (e instanceof ProtocolException) {
                emitOnError(HttpRequest.ERROR_INVALID_REQUEST_METHOD, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private boolean establishConnection() {
        try {
            mConnection.setConnectTimeout(mConnectTimeout);
            mConnection.connect();
            emitOnReadyStateChange(HttpRequest.STATE_OPENED);
            return true;
        } catch (IOException e) {
            if (e instanceof ConnectException) {
                if (e.getMessage().contains("ECONNREFUSED")) {
                    emitOnError(HttpRequest.ERROR_CONNECTION_REFUSED, e);
                } else if (e.getMessage().contains("ENETUNREACH")) {
                    emitOnError(HttpRequest.ERROR_NETWORK_UNREACHABLE, e);
                } else if (e.getMessage().contains("ETIMEDOUT")) {
                    emitOnError(HttpRequest.ERROR_CONNECTION_TIMED_OUT, e);
                } else {
                    emitOnError(HttpRequest.ERROR_UNKNOWN, e);
                }
            } else if (e instanceof SSLHandshakeException) {
                emitOnError(HttpRequest.ERROR_SSL_CERTIFICATE_INVALID, e);
            } else if (e instanceof SocketTimeoutException) {
                emitOnError(HttpRequest.ERROR_CONNECTION_TIMED_OUT, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    void sendRequest(final String contentType, final String data) {
        if (hasError()) return;
        mConnection.setRequestProperty("Content-Type", contentType);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!establishConnection()) return;
                if (data != null) {
                    if (!sendRequestData(data, true)) return;
                }
                readResponse();
            }
        }).start();
    }

    void sendRequest(final String contentType, final FormData data) {
        if (hasError()) return;
        mConnection.setRequestProperty("Content-Type", contentType);
        mConnection.setFixedLengthStreamingMode(data.getContentLength());
        mFilesCount = data.getFilesCount();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!establishConnection()) return;
                ArrayList<FormData.MultiPartData> requestItems = data.getData();
                for (FormData.MultiPartData item : requestItems) {
                    if (!sendRequestData(item.getPreContentData(), false)) break;
                    if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                        if (!sendRequestData(item.getContent(), false)) break;
                    } else {
                        mCurrentFileNumber += 1;
                        if (!writeContent(item.getContent())) break;
                    }
                    if (!sendRequestData(item.getPostContentData(), false)) break;
                }
                if (hasError()) return;
                if (!sendRequestData(FormData.FINISH_LINE, true)) return;
                readResponse();
            }
        }).start();
    }

    private void readResponse() {
        if (!assignResponseCodeAndMessage()) return;
        emitOnReadyStateChange(HttpRequest.STATE_HEADERS_RECEIVED);
        emitOnReadyStateChange(HttpRequest.STATE_LOADING);
        try {
            readFromInputStream(mConnection.getInputStream());
        } catch (IOException ignore) {
            readFromInputStream(mConnection.getErrorStream());
        }
        emitOnReadyStateChange(HttpRequest.STATE_DONE);
    }

    private boolean assignResponseCodeAndMessage() {
        try {
            mStatus = (short) mConnection.getResponseCode();
            Log.d(TAG, String.format("Request status: %s", mStatus));
            mStatusText = mConnection.getResponseMessage();
            return true;
        } catch (IOException e) {
            emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            Log.e(TAG, e.getMessage(), e);
            return false;
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
        } catch (IOException e) {
            emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private boolean sendRequestData(String body, boolean closeOnDone) {
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
            return true;
        } catch (IOException e) {
            if (e instanceof SocketException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private boolean writeContent(String uploadFilePath) {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
            if (mOutputStream == null) {
                mOutputStream = mConnection.getOutputStream();
            }
            FileInputStream inputStream = new FileInputStream(uploadFile);
            final byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
                uploaded += bytesRead;
                emitOnFileUploadProgress(uploadFile, uploaded, total);
            }
            return true;
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                if (e.getMessage().contains("ENOENT")) {
                    emitOnError(HttpRequest.ERROR_FILE_DOES_NOT_EXIST, e);
                } else if (e.getMessage().contains("EACCES")) {
                    emitOnError(HttpRequest.ERROR_FILE_READ_PERMISSION_DENIED, e);
                } else {
                    emitOnError(HttpRequest.ERROR_UNKNOWN, e);
                }
            } else if (e instanceof SocketException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private boolean hasError() {
        return mError > HttpRequest.ERROR_NONE;
    }
}
