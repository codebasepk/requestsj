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

public class HTTPRequest {

    private static final String TAG = HTTPRequest.class.getName();

    private final String CONTENT_TYPE_JSON = "application/json";
    private final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);

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

    public HTTPRequest() {
        mHandler = new Handler(Looper.getMainLooper());
        mThread = Executors.newSingleThreadExecutor();
        mHeaders = new HashMap<>();
    }

    public interface OnErrorListener {
        void onError(HTTPError error);
    }

    public interface OnFileUploadProgressListener {
        void onFileUploadProgress(File file, long uploaded, long total);
    }

    public interface OnResponseListener {
        void onResponse(HTTPResponse response);
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

    private void get(String url) {
        HTTP http = new HTTP();
        http.request("GET", url)
    }

    public void setTimeout(int timeout) {
        mConnectTimeout = timeout;
    }

    void emitOnResponse() {
        if (mOnResponseListener != null) {
            mHandler.post(() -> {
                HTTPResponse response = new HTTPResponse(mStatus, mStatusText, mResponseText, mUrl);
                mOnResponseListener.onResponse(response);
            });
        }
    }

    void emitOnFileUploadProgress(File file, long loaded, long total) {
        if (mOnFileUploadProgressListener != null) {
            mHandler.post(() -> mOnFileUploadProgressListener.onFileUploadProgress(
                    file, loaded, total));
        }
    }

    void emitOnError(short error, Exception exception) {
        if (mOnErrorListener != null) {
            mHandler.post(() -> {
                mOnErrorListener.onError(new HTTPError());
                Log.d(TAG, String.format("Emit Error: %s", error));
            });
        }
    }
}
