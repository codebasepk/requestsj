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

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import static pk.codebase.requests.HTTPError.CANNOT_SERIALIZE;
import static pk.codebase.requests.HTTPError.CONNECTION_REFUSED;
import static pk.codebase.requests.HTTPError.CONNECTION_TIMED_OUT;
import static pk.codebase.requests.HTTPError.FILE_DOES_NOT_EXIST;
import static pk.codebase.requests.HTTPError.FILE_READ_PERMISSION_DENIED;
import static pk.codebase.requests.HTTPError.INVALID_REQUEST_METHOD;
import static pk.codebase.requests.HTTPError.INVALID_URL;
import static pk.codebase.requests.HTTPError.LOST_CONNECTION;
import static pk.codebase.requests.HTTPError.NETWORK_UNREACHABLE;
import static pk.codebase.requests.HTTPError.SSL_CERTIFICATE_INVALID;
import static pk.codebase.requests.HTTPError.STAGE_CLEANING;
import static pk.codebase.requests.HTTPError.STAGE_CONNECTING;
import static pk.codebase.requests.HTTPError.STAGE_RECEIVING;
import static pk.codebase.requests.HTTPError.STAGE_SENDING;
import static pk.codebase.requests.HTTPError.UNKNOWN;

class HTTP {

    private static final String TAG = HTTP.class.getName();

    private HttpURLConnection mConn;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private int mFilesCount;
    private int mCurrentFileNumber;
    private String mStatusText;
    private String mResponseText;
    private short mStatus;
    private HTTPRequest.OnFileUploadProgressListener mFileProgressListener;

    HTTPResponse request(String method, String url, Object payloadRaw, Map<String, String> headers,
                         int connectTimeout, int readTimeout) throws HTTPError {
        int payloadLength = 0;
        Object payload = payloadRaw;
        if (payloadRaw != null) {
            if (payloadRaw instanceof FormData) {
                payloadLength = ((FormData) payloadRaw).getContentLength();
            } else if (payloadRaw instanceof String) {
                payloadLength = ((String) payloadRaw).getBytes().length;
            } else {
                try {
                    String pojoPayload = new ObjectMapper().writeValueAsString(payloadRaw);
                    payloadLength = pojoPayload.getBytes().length;
                    payload = pojoPayload;
                } catch (JsonProcessingException e) {
                    throw new HTTPError(CANNOT_SERIALIZE, STAGE_CONNECTING, e);
                }
            }
        }
        connect(method, url, payloadLength, headers, connectTimeout, readTimeout);
        System.out.println("Connected...");
        send(payload);
        System.out.println("Sent...");
        readResponse();
        cleanup();
        return new HTTPResponse(mStatus, mStatusText, mResponseText, url);
    }

    void setUploadProgressListener(HTTPRequest.OnFileUploadProgressListener listener) {
        mFileProgressListener = listener;
    }

    private void connect(String method, String endpoint, int payloadLength,
                         Map<String, String> headers, int connectTimeout, int readTimeout)
            throws HTTPError {
        try {
            URL url = new URL(endpoint);
            mConn = (HttpURLConnection) url.openConnection();
            mConn.setRequestMethod(method);
            mConn.setConnectTimeout(connectTimeout);
            mConn.setReadTimeout(readTimeout);
            if (headers != null) {
                for (Map.Entry<String, String> header: headers.entrySet()) {
                    mConn.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            mConn.setFixedLengthStreamingMode(payloadLength);
            mConn.connect();
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                throw new HTTPError(INVALID_URL, STAGE_CONNECTING, e);
            } else if (e instanceof ConnectException) {
                if (e.getMessage().contains("ECONNREFUSED")) {
                    throw new HTTPError(CONNECTION_REFUSED,
                            STAGE_CONNECTING, e);
                } else if (e.getMessage().contains("ENETUNREACH")) {
                    throw new HTTPError(NETWORK_UNREACHABLE,
                            STAGE_CONNECTING, e);
                } else if (e.getMessage().contains("ETIMEDOUT")) {
                    throw new HTTPError(CONNECTION_TIMED_OUT,
                            STAGE_CONNECTING, e);
                } else {
                    throw new HTTPError(UNKNOWN, STAGE_CONNECTING, e);
                }
            } else if (e instanceof SSLHandshakeException) {
                throw new HTTPError(SSL_CERTIFICATE_INVALID,
                        STAGE_CONNECTING, e);
            } else if (e instanceof SocketException) {
                throw new HTTPError(LOST_CONNECTION, STAGE_CONNECTING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(CONNECTION_TIMED_OUT, STAGE_CONNECTING, e);
            } else if (e instanceof ProtocolException) {
                throw new HTTPError(INVALID_REQUEST_METHOD, STAGE_CONNECTING, e);
            } else {
                throw new HTTPError(UNKNOWN, STAGE_CONNECTING, e);
            }
        }
    }

    private void send(Object payload) throws HTTPError {
        if (payload instanceof FormData) {
            sendRequest((FormData) payload);
        } else {
            sendRequest((String) payload);
        }
    }

    private void sendRequest(String data) throws HTTPError {
        if (data != null) {
            sendRequestData(data);
        }
    }

    private void sendRequest(FormData data) throws HTTPError {
        mFilesCount = data.getFilesCount();
        ArrayList<FormData.MultiPartData> requestItems = data.getData();
        for (FormData.MultiPartData item : requestItems) {
            sendRequestData(item.getPreContentData());
            if (item.getContentType() == FormData.TYPE_CONTENT_TEXT) {
                sendRequestData(item.getContent());
            } else {
                mCurrentFileNumber += 1;
                writeContent(item.getContent());
            }
            sendRequestData(item.getPostContentData());
        }
        sendRequestData(FormData.FINISH_LINE);
    }

    private void cleanup() throws HTTPError {
        try {
            if (mOutputStream != null) {
                mOutputStream.flush();
                mOutputStream.close();
            }
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            throw new HTTPError(UNKNOWN, STAGE_CLEANING, e);
        }
        mConn.disconnect();
    }

    private void readResponse() throws HTTPError {
        assignResponseCodeAndMessage();
        try {
            mInputStream = mConn.getInputStream();
        } catch (IOException ignore) {
            mInputStream = mConn.getErrorStream();
        }
        readFromInputStream();
    }

    private void assignResponseCodeAndMessage() throws HTTPError {
        try {
            Log.v(TAG, "Getting response headers");
            mStatus = (short) mConn.getResponseCode();
            Log.d(TAG, String.format("Response code: %s", mStatus));
            mStatusText = mConn.getResponseMessage();
        } catch (IOException e) {
            if (e instanceof SocketException) {
                throw new HTTPError(LOST_CONNECTION, STAGE_RECEIVING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(CONNECTION_TIMED_OUT, STAGE_RECEIVING, e);
            } else {
                throw new HTTPError(UNKNOWN, STAGE_RECEIVING, e);
            }
        }
    }

    private void readFromInputStream() throws HTTPError {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            mResponseText = output.toString();
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                throw new HTTPError(LOST_CONNECTION, STAGE_RECEIVING, e);
            } else {
                throw new HTTPError(UNKNOWN, STAGE_RECEIVING, e);
            }
        }
    }

    private void sendRequestData(String body) throws HTTPError {
        try {
            byte[] outputInBytes = body.getBytes();
            if (mOutputStream == null) {
                Log.v(TAG, "Getting OutputStream");
                mOutputStream = mConn.getOutputStream();
                Log.v(TAG, "Got OutputStream");
            }
            mOutputStream.write(outputInBytes);
            mOutputStream.flush();
        } catch (IOException e) {
            if (e instanceof SocketException) {
                throw new HTTPError(LOST_CONNECTION, STAGE_SENDING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(CONNECTION_TIMED_OUT, STAGE_SENDING, e);
            } else {
                throw new HTTPError(UNKNOWN, STAGE_SENDING, e);
            }
        }
    }

    private void writeContent(String uploadFilePath) throws HTTPError {
        File uploadFile = new File(uploadFilePath);
        long total = uploadFile.length();
        long uploaded = 0;
        try {
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
                    mFileProgressListener.onFileUploadProgress(uploadFile, uploaded, total);
                }
            }
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                if (e.getMessage().contains("ENOENT")) {
                    throw new HTTPError(FILE_DOES_NOT_EXIST, STAGE_SENDING, e);
                } else if (e.getMessage().contains("EACCES")) {
                    throw new HTTPError(FILE_READ_PERMISSION_DENIED,
                            STAGE_SENDING, e);
                } else {
                    throw new HTTPError(UNKNOWN, STAGE_SENDING, e);
                }
            } else if (e instanceof SocketException) {
                throw new HTTPError(LOST_CONNECTION, STAGE_SENDING, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(CONNECTION_TIMED_OUT, STAGE_SENDING, e);
            } else {
                throw new HTTPError(UNKNOWN, STAGE_SENDING, e);
            }
        }
    }
}
