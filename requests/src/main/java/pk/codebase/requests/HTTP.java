/*
 * Requests for Android
 * Copyright (C) 2019 CodeBasePK
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

package pk.codebase.requests;

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

class HTTP {

    private static final String TAG = HTTP.class.getName();

    private HttpURLConnection mConn;
    private OutputStream mOutputStream;
    private int mFilesCount;
    private int mCurrentFileNumber;
    private String mStatusText;
    private String mResponseText;
    private short mStatus;
    private HTTPRequest.OnFileUploadProgressListener mFileProgressListener;

    HTTPResponse request(String method, String url, String contentType, String payload,
                         int connectTimeout, int readTimeout) throws HTTPError {
        connect(url, connectTimeout, readTimeout);
        send(method, contentType, payload);
        readResponse();
        cleanup();
        return new HTTPResponse(mStatus, mStatusText, mResponseText, url);
    }

    void setUploadProgressListener(HTTPRequest.OnFileUploadProgressListener listener) {
        mFileProgressListener = listener;
    }

    private void connect(String endpoint, int connectTimeout, int readTimeout) throws HTTPError {
        try {
            URL url = new URL(endpoint);
            mConn = (HttpURLConnection) url.openConnection();
            mConn.setConnectTimeout(connectTimeout);
            mConn.setReadTimeout(readTimeout);
            mConn.connect();
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                throw new HTTPError(HTTPError.INVALID_URL, HTTPError.STAGE_CONNECTING, e);
            } else if (e instanceof ConnectException) {
                if (e.getMessage().contains("ECONNREFUSED")) {
                    throw new HTTPError(HTTPError.CONNECTION_REFUSED,
                            HTTPError.STAGE_CONNECTING, e);
                } else if (e.getMessage().contains("ENETUNREACH")) {
                    throw new HTTPError(HTTPError.NETWORK_UNREACHABLE,
                            HTTPError.STAGE_CONNECTING, e);
                } else if (e.getMessage().contains("ETIMEDOUT")) {
                    throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT,
                            HTTPError.STAGE_CONNECTING, e);
                } else {
                    throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_CONNECTING, e);
                }
            } else if (e instanceof SSLHandshakeException) {
                throw new HTTPError(HTTPError.SSL_CERTIFICATE_INVALID,
                        HTTPError.STAGE_CONNECTING, e);
            } else if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, HTTPError.STAGE_CONNECTING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, HTTPError.STAGE_CONNECTING, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_CONNECTING, e);
            }
        }
    }

    private void send(String method, String contentType, Object payload) throws HTTPError {
        try {
            mConn.setRequestMethod(method);
        } catch (ProtocolException e) {
            throw new HTTPError(HTTPError.INVALID_REQUEST_METHOD, HTTPError.STAGE_SENDING, e);
        }
        if (method != null && !method.equals("GET")) {
            mConn.setRequestProperty("Content-Type", contentType);
        }
        if (payload instanceof FormData) {
            sendRequest((FormData) payload);
        } else {
            sendRequest((String) payload);
        }
    }

    private void sendRequest(String data) throws HTTPError {
        if (data != null) {
            mConn.setFixedLengthStreamingMode(data.getBytes().length);
        }
        if (data != null) {
            sendRequestData(data);
        }
    }

    private void sendRequest(FormData data) throws HTTPError {
        mConn.setFixedLengthStreamingMode(data.getContentLength());
        mFilesCount = data.getFilesCount();
        ArrayList<FormData.MultiPartData> requestItems = data.getData();
        for (FormData.MultiPartData item : requestItems) {
            sendRequestData(item.getPreContentData());
            if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                sendRequestData(item.getContent());
            } else {
                mCurrentFileNumber += 1;
                writeContent(item.getContent());
            }
            sendRequestData(item.getPostContentData());
        }
        sendRequestData(FormData.FINISH_LINE);
    }

    private void cleanup() throws HTTPError {
        try {
            if (mOutputStream != null) {
                mOutputStream.flush();
                mOutputStream.close();
            }
        } catch (IOException e) {
            throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_CLEANING, e);
        }
        mConn.disconnect();
    }

    private void readResponse() throws HTTPError {
        assignResponseCodeAndMessage();
        try {
            readFromInputStream(mConn.getInputStream());
        } catch (IOException ignore) {
            readFromInputStream(mConn.getErrorStream());
        }
    }

    private void assignResponseCodeAndMessage() throws HTTPError {
        try {
            Log.v(TAG, "Getting response headers");
            mStatus = (short) mConn.getResponseCode();
            Log.d(TAG, String.format("Request status: %s", mStatus));
            mStatusText = mConn.getResponseMessage();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, HTTPError.STAGE_RECEIVING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, HTTPError.STAGE_RECEIVING, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_RECEIVING, e);
            }
        }
    }

    private void readFromInputStream(InputStream inputStream) throws HTTPError {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            mResponseText = output.toString();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, HTTPError.STAGE_RECEIVING, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_RECEIVING, e);
            }
        }
    }

    private void sendRequestData(String body) throws HTTPError {
        try {
            byte[] outputInBytes = body.getBytes();
            if (mOutputStream == null) {
                Log.v(TAG, "Getting OutputStream");
                mOutputStream = mConn.getOutputStream();
                Log.v(TAG, "Got OutputStream");
            }
            mOutputStream.write(outputInBytes);
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);

            if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, HTTPError.STAGE_SENDING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, HTTPError.STAGE_SENDING, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_SENDING, e);
            }
        }
    }

    private void writeContent(String uploadFilePath) throws HTTPError {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
            if (mOutputStream == null) {
                mOutputStream = mConn.getOutputStream();
            }
            FileInputStream inputStream = new FileInputStream(uploadFile);
            final byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
                uploaded += bytesRead;
                if (mFileProgressListener != null) {
                    mFileProgressListener.onFileUploadProgress(uploadFile, uploaded, total);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof FileNotFoundException) {
                if (e.getMessage().contains("ENOENT")) {
                    throw new HTTPError(HTTPError.FILE_DOES_NOT_EXIST, HTTPError.STAGE_SENDING, e);
                } else if (e.getMessage().contains("EACCES")) {
                    throw new HTTPError(HTTPError.FILE_READ_PERMISSION_DENIED,
                            HTTPError.STAGE_SENDING, e);
                } else {
                    throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_SENDING, e);
                }
            } else if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, HTTPError.STAGE_SENDING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, HTTPError.STAGE_SENDING, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, HTTPError.STAGE_SENDING, e);
            }
        }
    }
}
