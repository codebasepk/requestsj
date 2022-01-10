/*
 * Requests for Android
 * Copyright (C) 2019 CodeBasePK
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import static pk.codebase.requests.HttpError.CANNOT_SERIALIZE;
import static pk.codebase.requests.HttpError.CONNECTION_REFUSED;
import static pk.codebase.requests.HttpError.CONNECTION_TIMED_OUT;
import static pk.codebase.requests.HttpError.FILE_DOES_NOT_EXIST;
import static pk.codebase.requests.HttpError.FILE_READ_PERMISSION_DENIED;
import static pk.codebase.requests.HttpError.INVALID_REQUEST_METHOD;
import static pk.codebase.requests.HttpError.INVALID_URL;
import static pk.codebase.requests.HttpError.LOST_CONNECTION;
import static pk.codebase.requests.HttpError.NETWORK_UNREACHABLE;
import static pk.codebase.requests.HttpError.SSL_CERTIFICATE_INVALID;
import static pk.codebase.requests.HttpError.STAGE_CLEANING;
import static pk.codebase.requests.HttpError.STAGE_CONNECTING;
import static pk.codebase.requests.HttpError.STAGE_RECEIVING;
import static pk.codebase.requests.HttpError.STAGE_SENDING;
import static pk.codebase.requests.HttpError.STAGE_VALIDATING;
import static pk.codebase.requests.HttpError.UNKNOWN;

import android.net.InetAddresses;
import android.util.Log;

class HttpBase {

    private HttpURLConnection mConn;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private String mStatusText;
    private String mResponseText;
    private short mStatus;
    private HttpRequest.OnFileUploadProgressListener mFileProgressListener;
    private HttpFileUploadProgress mUploadProgress;

    HttpResponse request(String method, String urlRaw, Object payloadRaw, HttpHeaders headers,
                         HttpOptions options, HttpProxy httpProxy) throws HttpError {
        int payloadLength = 0;
        Object payload = payloadRaw;
        if (payloadRaw != null) {
            if (payloadRaw instanceof FormData) {
                payloadLength = ((FormData) payloadRaw).getContentLength();
            } else if (payloadRaw instanceof String) {
                payloadLength = ((String) payloadRaw).getBytes().length;
            } else if (payloadRaw instanceof JSONObject) {
                JSONObject obj = (JSONObject) payloadRaw;
                payload = obj.toString();
                payloadLength = ((String) payload).getBytes().length;
            } else if (payloadRaw instanceof JSONArray) {
                JSONArray obj = (JSONArray) payloadRaw;
                payload = obj.toString();
                payloadLength = ((String) payload).getBytes().length;
            } else {
                try {
                    String pojoPayload = new ObjectMapper().writeValueAsString(payloadRaw);
                    payloadLength = pojoPayload.getBytes().length;
                    payload = pojoPayload;
                } catch (Exception e) {
                    throw new HttpError(CANNOT_SERIALIZE, STAGE_VALIDATING, e);
                }
            }
        }
        URL url;
        try {
            url = new URL(urlRaw);
        } catch (Exception e) {
            throw new HttpError(INVALID_URL, STAGE_VALIDATING, e);
        }
        connect(method, url, payloadLength, headers, options, httpProxy);
        send(payload);
        readResponse();
        cleanup();
        return new HttpResponse(mStatus, mStatusText, mResponseText, urlRaw);
    }

    void setUploadProgressListener(HttpRequest.OnFileUploadProgressListener listener) {
        mFileProgressListener = listener;
    }

    private void connect(String method, URL url, int payloadLength,
                         HttpHeaders headers, HttpOptions options, final HttpProxy httpProxy) throws HttpError {
        try {
            if (httpProxy != null) {
                if (httpProxy.getUsername() != null && httpProxy.getPassword() != null) {
                    java.net.Authenticator authenticator = new java.net.Authenticator() {

                        protected java.net.PasswordAuthentication getPasswordAuthentication() {
                            return new java.net.PasswordAuthentication(httpProxy.getUsername(),
                                    httpProxy.getPassword().toCharArray());
                        }
                    };

                    System.setProperty("java.net.socks.username", httpProxy.getUsername());
                    System.setProperty("java.net.socks.password", httpProxy.getPassword());
                    java.net.Authenticator.setDefault(authenticator);
                }
                SocketAddress socketAddress = new InetSocketAddress(httpProxy.getHost(), httpProxy.getPort());
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socketAddress);
                mConn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                mConn = (HttpURLConnection) url.openConnection();
            }
            mConn.setRequestMethod(method);
            mConn.setConnectTimeout(options.connectTimeout);
            mConn.setReadTimeout(options.readTimeout);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                mConn.setRequestProperty(header.getKey(), header.getValue());
            }
            mConn.setFixedLengthStreamingMode(payloadLength);
            mConn.connect();
        } catch (Exception e) {
            HttpError error = new HttpError(STAGE_CONNECTING, e);
            if (e instanceof ConnectException) {
                if (e.getMessage().contains("ECONNREFUSED")) {
                    error.setCode(CONNECTION_REFUSED);
                } else if (e.getMessage().contains("ENETUNREACH")) {
                    error.setCode(NETWORK_UNREACHABLE);
                } else if (e.getMessage().contains("ETIMEDOUT")) {
                    error.setCode(CONNECTION_TIMED_OUT);
                } else {
                    error.setCode(UNKNOWN);
                }
            } else if (e instanceof SSLHandshakeException) {
                error.setCode(SSL_CERTIFICATE_INVALID);
            } else if (e instanceof SocketException) {
                error.setCode(LOST_CONNECTION);
            } else if (e instanceof SocketTimeoutException) {
                error.setCode(CONNECTION_TIMED_OUT);
            } else if (e instanceof ProtocolException) {
                error.setCode(INVALID_REQUEST_METHOD);
            } else {
                error.setCode(UNKNOWN);
            }
            throw error;
        }
    }

    private void send(Object payload) throws HttpError {
        try {
            if (payload instanceof FormData) {
                sendForm((FormData) payload);
            } else {
                String data = (String) payload;
                if (data != null) {
                    write(data);
                }
            }
        } catch (Exception e) {
            HttpError error = new HttpError(STAGE_SENDING, e);
            if (e instanceof SocketException) {
                error.setCode(LOST_CONNECTION);
            } else if (e instanceof SocketTimeoutException) {
                error.setCode(CONNECTION_TIMED_OUT);
            } else if (e instanceof FileNotFoundException) {
                if (e.getMessage().contains("ENOENT")) {
                    error.setCode(FILE_DOES_NOT_EXIST);
                } else if (e.getMessage().contains("EACCES")) {
                    error.setCode(FILE_READ_PERMISSION_DENIED);
                }
            }
            throw error;
        }
    }

    private void sendForm(FormData data) throws Exception {
        mUploadProgress = new HttpFileUploadProgress(data.getFilesCount());
        int currentFileNumber = 0;
        ArrayList<FormData.MultiPartData> requestItems = data.getData();
        for (FormData.MultiPartData item : requestItems) {
            write(item.getPreContentData());
            if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                write(item.getContent());
            } else {
                currentFileNumber += 1;
                mUploadProgress.setFileNumber(currentFileNumber);
                writeContent(item.getContent());
            }
            write(item.getPostContentData());
        }
        write(FormData.FINISH_LINE);
    }

    private void write(String body) throws Exception {
        if (mOutputStream == null) {
            mOutputStream = mConn.getOutputStream();
        }
        mOutputStream.write(body.getBytes());
        mOutputStream.flush();
    }

    private void writeContent(String uploadFilePath) throws Exception {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        if (mOutputStream == null) {
            mOutputStream = mConn.getOutputStream();
        }
        FileInputStream inputStream = new FileInputStream(uploadFile);
        final byte[] buffer = new byte[512];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            mOutputStream.write(buffer, 0, bytesRead);
            mOutputStream.flush();
            uploaded += bytesRead;
            if (mFileProgressListener != null) {
                mUploadProgress.setCurrentFile(uploadFile);
                mUploadProgress.setUploaded(uploaded);
                mUploadProgress.setTotal(total);
                mFileProgressListener.onFileUploadProgress(mUploadProgress);
            }
        }
    }

    private void readResponse() throws HttpError {
        try {
            mInputStream = mConn.getInputStream();
        } catch (IOException ignore) {
            mInputStream = mConn.getErrorStream();
        }
        try {
            mStatus = (short) mConn.getResponseCode();
            mStatusText = mConn.getResponseMessage();
            readFromInputStream();
        } catch (Exception e) {
            if (e instanceof SocketException) {
                throw new HttpError(LOST_CONNECTION, STAGE_RECEIVING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HttpError(CONNECTION_TIMED_OUT, STAGE_RECEIVING, e);
            } else {
                throw new HttpError(UNKNOWN, STAGE_RECEIVING, e);
            }
        }
    }

    private void readFromInputStream() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append('\n');
        }
        mResponseText = output.toString();
    }

    private void cleanup() throws HttpError {
        try {
            if (mOutputStream != null) {
                mOutputStream.flush();
                mOutputStream.close();
            }
            if (mInputStream != null) {
                mInputStream.close();
            }
            mConn.disconnect();
        } catch (Exception e) {
            throw new HttpError(UNKNOWN, STAGE_CLEANING, e);
        }
    }
}
