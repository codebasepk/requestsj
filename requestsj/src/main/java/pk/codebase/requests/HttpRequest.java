/*
 * Requests for Android
 * Copyright (C) 2016-2019 CodeBasePK
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpRequest {

    // Stole from HttpURLConnection
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_OK = 200;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_VERSION = 505;

    private static final String TAG = HttpRequest.class.getName();

    private final String CONTENT_TYPE_JSON = "application/json";
    private final String CONTENT_TYPE_FORM = String.format(
            "multipart/form-data; boundary=%s", FormData.BOUNDARY);
    private final String CONTENT_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";

    private OnErrorListener mOnErrorListener;
    private OnFileUploadProgressListener mOnFileUploadProgressListener;
    private OnResponseListener mOnResponseListener;

    private ExecutorService mThread;
    private Handler mHandler;
    private final String mBaseURL;

    public HttpRequest() {
        this("");
    }

    public HttpRequest(String baseURL) {
        mHandler = new Handler(Looper.getMainLooper());
        mThread = Executors.newSingleThreadExecutor();
        mBaseURL = baseURL;
    }

    public interface OnErrorListener {
        void onError(HttpError error);
    }

    public interface OnFileUploadProgressListener {
        void onFileUploadProgress(HttpUploadProgress progress);
    }

    public interface OnResponseListener {
        void onResponse(HttpResponse response);
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

    private void request(String method, String rawURL, Object payload, HttpHeaders headers,
                         HttpOptions options) {
        String url = rawURL;
        if (url != null) {
            if (!url.startsWith("http") && !mBaseURL.isEmpty()) {
                if (mBaseURL.endsWith("/") && url.startsWith("/")) {
                    url = String.format("%s%s", mBaseURL, url.substring(1));
                } else if (!mBaseURL.endsWith("/") && !url.startsWith("/")) {
                    url = String.format("%s/%s", mBaseURL, url);
                } else {
                    url = String.format("%s%s", mBaseURL, url);
                }
            }
        }
        HttpBase http = new HttpBase();
        http.setUploadProgressListener(new OnFileUploadProgressListener() {
            @Override
            public void onFileUploadProgress(HttpUploadProgress progress) {
                emitOnFileUploadProgress(progress);
            }
        });
        Map<String, String> actualHeaders = headers;
        if (actualHeaders == null) {
            actualHeaders = new HashMap<>();
        }
        HttpOptions actualOptions = options;
        if (actualOptions == null) {
            actualOptions = new HttpOptions();
        }
        if (!method.equals("GET")) {
            if (payload instanceof FormData) {
                actualHeaders.put("Content-Type", CONTENT_TYPE_FORM);
            } else if (!actualHeaders.containsKey("Content-Type") ||
                    !actualHeaders.containsKey("content-type")) {
                actualHeaders.put("Content-Type", CONTENT_TYPE_JSON);
            }
        }
        try {
            HttpResponse response = http.request(method, url, payload, actualHeaders,
                    actualOptions.connectTimeout, actualOptions.readTimeout);
            emitOnResponse(response);
        } catch (HttpError error) {
            emitOnError(error);
        } catch (Exception e) {
            emitOnError(new HttpError(HttpError.UNKNOWN, HttpError.STAGE_UNKNOWN, e));
        }
    }

    public void get(String url) {
        get(url, null, null);
    }

    public void get(String url, HttpHeaders headers) {
        get(url, headers, null);
    }

    public void get(String url, HttpOptions options) {
        get(url, null, options);
    }

    public void get(final String url, final HttpHeaders headers,
                    final HttpOptions options) {
        mThread.submit(new Runnable() {
            @Override
            public void run() {
                request("GET", url, null, headers, options);
            }
        });
    }

    public void post(String url) {
        post(url, null, null, null);
    }

    public void post(String url, HttpHeaders headers) {
        post(url, null, headers, null);
    }

    public void post(String url, HttpOptions options) {
        post(url, null, null, options);
    }

    public void post(String url, Object payload) {
        post(url, payload, null, null);
    }

    public void post(String url, Object payload, HttpHeaders headers) {
        post(url, payload, headers, null);
    }

    public void post(String url, Object payload, HttpOptions options) {
        post(url, payload, null, options);
    }

    public void post(final String url, final Object payload, final HttpHeaders headers,
                     final HttpOptions options) {
         mThread.submit(new Runnable() {
             @Override
             public void run() {
                 request("POST", url, payload, headers, options);
             }
         });
    }

    public void put(String url) {
        put(url, null, null, null);
    }

    public void put(String url, HttpHeaders headers) {
        put(url, null, headers, null);
    }

    public void put(String url, HttpOptions options) {
        put(url, null, null, options);
    }

    public void put(String url, Object payload) {
        put(url, payload, null, null);
    }

    public void put(String url, Object payload, HttpHeaders headers) {
        put(url, payload, headers, null);
    }

    public void put(String url, Object payload, HttpOptions options) {
        put(url, payload, null, options);
    }

    public void put(final String url, final Object payload, final HttpHeaders headers,
                    final HttpOptions options) {
        mThread.submit(new Runnable() {
            @Override
            public void run() {
                request("PUT", url, payload, headers, options);
            }
        });
    }

    public void patch(String url) {
        patch(url, null, null, null);
    }

    public void patch(String url, HttpHeaders headers) {
        patch(url, null, headers, null);
    }

    public void patch(String url, HttpOptions options) {
        patch(url, null, null, options);
    }

    public void patch(String url, Object payload) {
        patch(url, payload, null, null);
    }

    public void patch(String url, Object payload, HttpHeaders headers) {
        patch(url, payload, headers, null);
    }

    public void patch(String url, Object payload, HttpOptions options) {
        patch(url, payload, null, options);
    }

    public void patch(final String url, final Object payload, final HttpHeaders headers,
                      final HttpOptions options) {
        mThread.submit(new Runnable() {
            @Override
            public void run() {
                request("PATCH", url, payload, headers, options);
            }
        });
    }

    public void delete(String url) {
        delete(url, null, null, null);
    }

    public void delete(String url, HttpHeaders headers) {
        delete(url, null, headers, null);
    }

    public void delete(String url, HttpOptions options) {
        delete(url, null, null, options);
    }

    public void delete(String url, Object payload) {
        delete(url, payload, null, null);
    }

    public void delete(String url, Object payload, HttpHeaders headers) {
        delete(url, payload, headers, null);
    }

    public void delete(String url, Object payload, HttpOptions options) {
        delete(url, payload, null, options);
    }

    public void delete(final String url, final Object payload, final HttpHeaders headers,
                       final HttpOptions options) {
        mThread.submit(new Runnable() {
            @Override
            public void run() {
                request("DELETE", url, payload, headers, options);
            }
        });
    }

    private void emitOnResponse(final HttpResponse response) {
        if (mOnResponseListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnResponseListener.onResponse(response);
                }
            });
        }
    }

    private void emitOnFileUploadProgress(final HttpUploadProgress progress) {
        if (mOnFileUploadProgressListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnFileUploadProgressListener.onFileUploadProgress(progress);
                }
            });
        }
    }

    private void emitOnError(final HttpError error) {
        if (mOnErrorListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnErrorListener.onError(error);
                }
            });
        }
    }
}
