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

package pk.codebase.requests;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPRequest {

    private static final String TAG = HTTPRequest.class.getName();

    private final String CONTENT_TYPE_JSON = "application/json";
    private final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);

    private OnErrorListener mOnErrorListener;
    private OnFileUploadProgressListener mOnFileUploadProgressListener;
    private OnResponseListener mOnResponseListener;

    private int mConnectTimeout = 15000;
    private ExecutorService mThread;
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

    public void get(String url) {
        mThread.submit(() -> {
            try {
                HTTP http = new HTTP();
                http.setUploadProgressListener(this::emitOnFileUploadProgress);
                System.out.println("HEREERERER>........");
                HTTPResponse res = http.request("GET", url, null, null, mConnectTimeout,
                        mConnectTimeout);
                System.out.println("Sending...");
                emitOnResponse(res);
            } catch (HTTPError error) {
                emitOnError(error);
            }
        });
    }

    void emitOnResponse(HTTPResponse response) {
        if (mOnResponseListener != null) {
            mHandler.post(() -> mOnResponseListener.onResponse(response));
        }
    }

    void emitOnFileUploadProgress(File file, long loaded, long total) {
        if (mOnFileUploadProgressListener != null) {
            mHandler.post(() -> mOnFileUploadProgressListener.onFileUploadProgress(
                    file, loaded, total));
        }
    }

    void emitOnError(HTTPError error) {
        if (mOnErrorListener != null) {
            mHandler.post(() -> mOnErrorListener.onError(error));
        }
    }
}
