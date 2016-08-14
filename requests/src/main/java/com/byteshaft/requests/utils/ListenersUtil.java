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
import android.os.Handler;

import com.byteshaft.requests.HttpRequest;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class ListenersUtil {

    private Handler mMainHandler;
    private static ListenersUtil sListenersUtil;

    public static ListenersUtil getInstance(Context context) {
        if (sListenersUtil == null) {
            sListenersUtil = new ListenersUtil(context);
        }
        return sListenersUtil;
    }

    private ListenersUtil(Context context) {
        mMainHandler = new Handler(context.getMainLooper());
    }

    protected void emitOnReadyStateChange(
            ArrayList<HttpRequest.OnReadyStateChangeListener> listeners,
            final HttpURLConnection connection,
            final int readyState
    ) {
        for (final HttpRequest.OnReadyStateChangeListener listener : listeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onReadyStateChange(connection, readyState);
                }
            });
        }
    }

    protected void emitOnFileUploadProgress(
            ArrayList<HttpRequest.FileUploadProgressListener> listeners,
            final File file,
            final long uploaded,
            final long total
    ) {
        for (final HttpRequest.FileUploadProgressListener listener : listeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFileUploadProgress(file, uploaded, total);
                }
            });
        }
    }
}
