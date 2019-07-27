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
    private Map<String, String> mHeaders;

    public HttpRequest() {
        mHandler = new Handler(Looper.getMainLooper());
        mThread = Executors.newSingleThreadExecutor();
        mHeaders = new HashMap<>();
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

    private void request(String method, String url, Object payload, Map<String, String> headers,
                         HttpOptions options) {
        HttpBase http = new HttpBase();
        http.setUploadProgressListener(this::emitOnFileUploadProgress);
        Map<String, String> actualHeaders = headers;
        if (actualHeaders == null) {
            actualHeaders = new HashMap<>();
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
                    options.connectTimeout, options.readTimeout);
            emitOnResponse(response);
        } catch (HttpError error) {
            emitOnError(error);
        } catch (Exception e) {
            emitOnError(new HttpError(HttpError.UNKNOWN, HttpError.STAGE_UNKNOWN, e));
        }
    }

    private void actuallyGet(String url, Map<String, String> headers, HttpOptions options) {
        mThread.submit(() -> request("GET", url, null, headers, options));
    }

    public void get(String url) {
        actuallyGet(url, null, new HttpOptions());
    }

    public void get(String url, Map<String, String> headers) {
        actuallyGet(url, headers, new HttpOptions());
    }

    public void get(String url, HttpOptions options) {
        actuallyGet(url, null, options);
    }

    public void get(String url, Map<String, String> headers, HttpOptions options) {
        actuallyGet(url, headers, options);
    }

    private void actuallyPost(String url, Object payload, Map<String, String> headers,
                              HttpOptions options) {
        mThread.submit(() -> request("POST", url, payload, headers, options));
    }

    public void post(String url) {
        actuallyPost(url, null, new HashMap<>(), new HttpOptions());
    }

    public void post(String url, String payload) {
        actuallyPost(url, payload, new HashMap<>(), new HttpOptions());
    }

    public void post(String url, Map<String, String> headers) {
        actuallyPost(url, null, headers, new HttpOptions());
    }

    public void post(String url, HttpOptions options) {
        actuallyPost(url, null, new HashMap<>(), options);
    }

    public void post(String url, String payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HttpOptions());
    }

    public void post(String url, FormData payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HttpOptions());
    }

     public void post(String url, JSONObject payload, Map<String, String> headers) {
         actuallyPost(url, payload, headers, new HttpOptions());
    }

     public void post(String url, JSONArray payload, Map<String, String> headers) {
        actuallyPost(url, payload, headers, new HttpOptions());
    }

     public void post(String url, Object pojo, Map<String, String> headers, HttpOptions options) {
         actuallyPost(url, pojo, headers, options);
    }

    private void actuallyDelete(String url, Object payload, Map<String, String> headers,
                                HttpOptions options) {
        mThread.submit(() -> request("DELETE", url, payload, headers, options));
    }

    public void delete(String url, Object payload, Map<String, String> headers,
                       HttpOptions options) {
        actuallyDelete(url, payload, headers, options);
    }

    private void actuallyPut(String url, Object payload, Map<String, String> headers,
                                HttpOptions options) {
        mThread.submit(() -> request("PUT", url, payload, headers, options));
    }

    public void put(String url, Object payload, Map<String, String> headers, HttpOptions options) {
        actuallyPut(url, payload, headers, options);
    }

    private void actuallyPatch(String url, Object payload, Map<String, String> headers,
                             HttpOptions options) {
        mThread.submit(() -> request("PATCH", url, payload, headers, options));
    }

    public void patch(String url, Object payload, Map<String, String> headers,
                      HttpOptions options) {
        actuallyPatch(url, payload, headers, options);
    }

    private void emitOnResponse(HttpResponse response) {
        if (mOnResponseListener != null) {
            mHandler.post(() -> mOnResponseListener.onResponse(response));
        }
    }

    private void emitOnFileUploadProgress(HttpUploadProgress progress) {
        if (mOnFileUploadProgressListener != null) {
            mHandler.post(() -> mOnFileUploadProgressListener.onFileUploadProgress(progress));
        }
    }

    private void emitOnError(HttpError error) {
        if (mOnErrorListener != null) {
            mHandler.post(() -> mOnErrorListener.onError(error));
        }
    }
}
