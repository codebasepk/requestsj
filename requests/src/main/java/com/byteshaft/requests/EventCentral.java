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
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

class EventCentral {

    private static final String TAG = "EventCentral";
    private final ArrayList<HttpRequest.OnErrorListener> mOnErrorListeners;
    private final ArrayList<HttpRequest.OnFileUploadProgressListener> mOnFileUploadProgressListeners;
    private final ArrayList<HttpRequest.OnReadyStateChangeListener> mOnReadyStateChangeListeners;
    private final Handler mMainHandler;

    short mError = HttpRequest.ERROR_NONE;
    short mReadyState = HttpRequest.STATE_UNSET;
    HttpRequest mRequest;

    EventCentral(Context context) {
        mOnErrorListeners = new ArrayList<>();
        mOnFileUploadProgressListeners = new ArrayList<>();
        mOnReadyStateChangeListeners = new ArrayList<>();
        mMainHandler = new Handler(context.getMainLooper());
    }

    void emitOnReadyStateChange(final short readyState) {
        mReadyState = readyState;
        for (final HttpRequest.OnReadyStateChangeListener listener : mOnReadyStateChangeListeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onReadyStateChange(mRequest, readyState);
                }
            });
        }
        Log.d(TAG, String.format("Emit readyState: %s", mReadyState));
    }

    void emitOnFileUploadProgress(final File file, final long loaded, final long total) {
        for (final HttpRequest.OnFileUploadProgressListener listener : mOnFileUploadProgressListeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFileUploadProgress(mRequest, file, loaded, total);
                }
            });
        }
    }

    void emitOnError(short error, final Exception exception) {
        mError = error;
        for (final HttpRequest.OnErrorListener listener : mOnErrorListeners) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(mRequest, mReadyState, mError, exception);
                }
            });
        }
        Log.d(TAG, String.format("Emit Error: %s", mError));
    }

    void addOnErrorListener(HttpRequest.OnErrorListener listener) {
        mOnErrorListeners.add(listener);
    }

    void addOnProgressUpdateListener(HttpRequest.OnFileUploadProgressListener listener) {
        mOnFileUploadProgressListeners.add(listener);
    }

    void addOnReadyStateListener(HttpRequest.OnReadyStateChangeListener listener) {
        mOnReadyStateChangeListeners.add(listener);
    }
}
