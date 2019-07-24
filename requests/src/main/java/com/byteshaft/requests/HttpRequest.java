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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLHandshakeException;

public class HttpRequest {

    private static final String TAG = HttpRequest.class.getName();

    private final String CONTENT_TYPE_JSON = "application/json";
    private final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);

    public static final short ERROR_NONE = -1;
    public static final short ERROR_UNKNOWN = 0;
    public static final short ERROR_INVALID_URL = 1;
    public static final short ERROR_INVALID_REQUEST_METHOD = 2;
    public static final short ERROR_CONNECTION_REFUSED = 3;
    public static final short ERROR_SSL_CERTIFICATE_INVALID = 4;
    public static final short ERROR_FILE_DOES_NOT_EXIST = 5;
    public static final short ERROR_FILE_READ_PERMISSION_DENIED = 6;
    public static final short ERROR_NETWORK_UNREACHABLE = 7;
    public static final short ERROR_CONNECTION_TIMED_OUT = 8;
    public static final short ERROR_LOST_CONNECTION = 9;

    private OnErrorListener mOnErrorListener;
    private OnFileUploadProgressListener mOnFileUploadProgressListener;
    private OnResponseListener mOnResponseListener;

    private HttpURLConnection mConnection;
    private int mConnectTimeout;
    private String mStatusText;
    private String mResponseText;
    private String mUrl;
    private int mFilesCount;
    private int mCurrentFileNumber;
    private int mStatus;
    private ExecutorService mThread;
    private BufferedOutputStream mOutputStream;
    private Handler mHandler;
    private Map<String, String> mHeaders;

    public HttpRequest() {
        mHandler = new Handler(Looper.getMainLooper());
        mThread = Executors.newSingleThreadExecutor();
        mHeaders = new HashMap<>();
    }

    public interface OnErrorListener {
        void onError(HttpResponse response, int error, Exception exception);
    }

    public interface OnFileUploadProgressListener {
        void onFileUploadProgress(File file, long loaded, long total);
    }

    public interface OnResponseListener {
        void onResponse(HttpResponse response, int status);
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnFileUploadProgressListener(OnFileUploadProgressListener listener) {
        mOnFileUploadProgressListener = listener;
    }

    public void setOnResponseListener(OnResponseListener listener) {
        mOnResponseListener = listener;
    }

    private boolean open(String requestMethod, String url) {
        mUrl = url;
        try {
            URL urlObject = new URL(mUrl);
            mConnection = (HttpURLConnection) urlObject.openConnection();
            mConnection.setRequestMethod(requestMethod);
            for (Map.Entry<String, String> entry: mHeaders.entrySet()) {
                mConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            return true;
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                emitOnError(HttpRequest.ERROR_INVALID_URL, e);
            } else if (e instanceof ProtocolException) {
                emitOnError(HttpRequest.ERROR_INVALID_REQUEST_METHOD, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private boolean establishConnection() {
        try {
            mConnection.setConnectTimeout(mConnectTimeout);
            mConnection.setReadTimeout(mConnectTimeout);
            mConnection.connect();
            mOutputStream = new BufferedOutputStream(mConnection.getOutputStream());
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
            } else if (e instanceof SocketException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
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
        mConnection.setRequestProperty("Content-Type", contentType);
        if (data != null) {
            mConnection.setFixedLengthStreamingMode(data.getBytes().length);
        }
        mThread.submit(() -> {
            if (!establishConnection()) return;
            if (data != null) {
                if (!sendData(data, true)) return;
            }
            readResponse();
        });
    }

    void sendRequest(final String contentType, final FormData data) {
        mConnection.setRequestProperty("Content-Type", contentType);
        mConnection.setFixedLengthStreamingMode(data.getContentLength());
        mFilesCount = data.getFilesCount();
        mThread.submit(() -> {
            if (!establishConnection()) return;
            ArrayList<FormData.MultiPartData> requestItems = data.getData();
            for (FormData.MultiPartData item : requestItems) {
                if (!sendData(item.getPreContentData(), false)) break;
                if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                    if (!sendData(item.getContent(), false)) break;
                } else {
                    mCurrentFileNumber += 1;
                    if (!sendData(item.getContent())) break;
                }
                if (!sendData(item.getPostContentData(), false)) break;
            }
            if (!sendData(FormData.FINISH_LINE, true)) return;
            readResponse();
        });
    }

    private boolean sendData(String body, boolean closeOnDone) {
        try {
            byte[] outputInBytes = body.getBytes();
            mOutputStream.write(outputInBytes);
            mOutputStream.flush();
            if (closeOnDone) {
                cleanup();
            }
            return true;
        } catch (IOException e) {
            if (e instanceof SocketException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                emitOnError(HttpRequest.ERROR_CONNECTION_TIMED_OUT, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private boolean sendData(String uploadFilePath) {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
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
            } else if (e instanceof SocketTimeoutException) {
                emitOnError(HttpRequest.ERROR_CONNECTION_TIMED_OUT, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    private void readResponse() {
        cleanup();
        if (!assignResponseCodeAndMessage()) return;
        try {
            readFromInputStream(mConnection.getInputStream());
        } catch (IOException ignore) {
            readFromInputStream(mConnection.getErrorStream());
        }
        emitOnResponse();
    }

    private void cleanup() {
        try {
            mOutputStream.flush();
            mOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean assignResponseCodeAndMessage() {
        try {
            Log.v(TAG, "Getting response headers");
            mStatus = mConnection.getResponseCode();
            Log.d(TAG, String.format("Request status: %s", mStatus));
            mStatusText = mConnection.getResponseMessage();
            return true;
        } catch (IOException e) {
            if (e instanceof SocketException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                emitOnError(HttpRequest.ERROR_CONNECTION_TIMED_OUT, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
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
            mConnection.disconnect();
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                emitOnError(HttpRequest.ERROR_LOST_CONNECTION, e);
            } else {
                emitOnError(HttpRequest.ERROR_UNKNOWN, e);
            }
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void setRequestHeader(String key, String value) {
        if (mConnection == null) {
            mHeaders.put(key, value);
        } else {
            mConnection.setRequestProperty(key, value);
        }
    }

    public void setTimeout(int timeout) {
        mConnectTimeout = timeout;
    }

    public void send(String method, String url, String data) {
        open(method, url);
        sendRequest(CONTENT_TYPE_JSON, data);
    }

    public void send(String method, String url) {
        open(method, url);
        sendRequest(CONTENT_TYPE_JSON, (String) null);
    }

    public void send(String method, String url, FormData formData) {
        open(method, url);
        sendRequest(CONTENT_TYPE_FORM, formData);
    }

    void emitOnResponse() {
        if (mOnResponseListener == null) {
            return;
        }
        mHandler.post(() -> {
            HttpResponse response = new HttpResponse(
                    mFilesCount, mCurrentFileNumber, mConnectTimeout, mStatus, mStatusText,
                    mResponseText, mUrl);
            mOnResponseListener.onResponse(response, 1);
        });
    }

    void emitOnFileUploadProgress(File file, long loaded, long total) {
        if (mOnFileUploadProgressListener == null) {
            return;
        }
        mHandler.post(() -> mOnFileUploadProgressListener.onFileUploadProgress(file, loaded, total));
    }

    void emitOnError(short error, Exception exception) {
        if (mOnErrorListener == null) {
            return;
        }
        mHandler.post(() -> {
            HttpResponse response = new HttpResponse(
                    mFilesCount, mCurrentFileNumber, mConnectTimeout, mStatus, mStatusText,
                    mResponseText, mUrl);
            mOnErrorListener.onError(response, error, exception);
            Log.d(TAG, String.format("Emit Error: %s", error));
        });
    }
}
