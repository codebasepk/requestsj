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
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;

class EventEmitter {

    private Handler mMainHandler;
    private static EventEmitter sListenersUtil;

    static EventEmitter getInstance(Context context) {
        if (sListenersUtil == null) {
            sListenersUtil = new EventEmitter(context);
        }
        return sListenersUtil;
    }

    private EventEmitter(Context context) {
        mMainHandler = new Handler(context.getMainLooper());
    }

    void emitOnReadyStateChange(
            ArrayList<HttpRequest.OnReadyStateChangeListener> listeners,
            final HttpRequest request,
            final int readyState
    ) {
        for (final HttpRequest.OnReadyStateChangeListener listener : listeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onReadyStateChange(request, readyState);
                }
            });
        }
    }

    void emitOnFileUploadProgress(
            ArrayList<HttpRequest.OnFileUploadProgressListener> listeners,
            final HttpRequest request,
            final File file,
            final long loaded,
            final long total
    ) {
        for (final HttpRequest.OnFileUploadProgressListener listener : listeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFileUploadProgress(request, file, loaded, total);
                }
            });
        }
    }

    void emitOnError(
            ArrayList<HttpRequest.OnErrorListener> listeners,
            final HttpRequest request
    ) {
        for (final HttpRequest.OnErrorListener listener : listeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(request);
                }
            });
        }
    }
}
