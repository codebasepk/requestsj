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

import com.byteshaft.requests.utils.HttpRequestUtil;

import java.io.File;
import java.net.HttpURLConnection;

public class HttpRequest extends HttpRequestUtil {

    public static final short STATE_UNSET = 0;
    public static final short STATE_OPENED = 1;
    public static final short STATE_HEADERS_RECEIVED = 2;
    public static final short STATE_LOADING = 3;
    public static final short STATE_DONE = 4;
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY
    );

    public HttpRequest(Context context) {
        super(context);
    }

    public interface FileUploadProgressListener {
        void onFileUploadProgress(File file, long uploaded, long total);
    }

    public interface OnReadyStateChangeListener {
        void onReadyStateChange(HttpURLConnection connection, int readyState);
    }

    public void setOnReadyStateChangeListener(OnReadyStateChangeListener listener) {
        addReadyStateListener(listener);
    }

    public void setOnFileUploadProgressListener(FileUploadProgressListener listener) {
        addProgressUpdateListener(listener);
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
}
