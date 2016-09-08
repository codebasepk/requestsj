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

import java.io.File;
import java.net.HttpURLConnection;

public class HttpRequest extends BaseHttpRequest {

    public static final short STATE_UNSET = 0;
    public static final short STATE_OPENED = 1;
    public static final short STATE_HEADERS_RECEIVED = 2;
    public static final short STATE_LOADING = 3;
    public static final short STATE_DONE = 4;

    public HttpRequest(Context context) {
        super(context);
        mRequest = this;
    }

    public interface OnErrorListener {
        void onError(HttpRequest request);
    }

    public interface OnFileUploadProgressListener {
        void onFileUploadProgress(HttpRequest request, File file, long loaded, long total);
    }

    public interface OnReadyStateChangeListener {
        void onReadyStateChange(HttpRequest request, int readyState);
    }

    public void setOnErrorListener(OnErrorListener listener) {
        addOnErrorListener(listener);
    }

    public void setOnFileUploadProgressListener(OnFileUploadProgressListener listener) {
        addOnProgressUpdateListener(listener);
    }

    public void setOnReadyStateChangeListener(OnReadyStateChangeListener listener) {
        addOnReadyStateListener(listener);
    }

    public void open(String requestMethod, String url) {
        openConnection(requestMethod, url);
    }

    public void setRequestHeader(String key, String value) {
        if (mConnection == null) {
            throw new RuntimeException("open() must be called first.");
        }
        mConnection.setRequestProperty(key, value);
    }

    public void send(String data) {
        sendRequest(CONTENT_TYPE_JSON, data);
    }

    public void send() {
        sendRequest(CONTENT_TYPE_JSON, (String) null);
    }

    public void send(FormData formData) {
        sendRequest(CONTENT_TYPE_FORM, formData);
    }

    public String getResponseText() {
        return mResponseText;
    }

    public HttpURLConnection getConnection() {
        return mConnection;
    }

    public short getReadyState() {
        return mReadyState;
    }

    public String getResponseURL() {
        return mUrl;
    }

    public short getStatus() {
        return mStatus;
    }

    public String getStatusText() {
        return mStatusText;
    }

    public String getResponseHeader(String headerName) {
        return mConnection.getHeaderField(headerName);
    }

    public int getTotalFiles() {
        return mFilesCount;
    }

    public int getCurrentFileNumber() {
        return mCurrentFileNumber;
    }
}
