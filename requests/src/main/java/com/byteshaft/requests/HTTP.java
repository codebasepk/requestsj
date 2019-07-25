package com.byteshaft.requests;

import android.util.Log;

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

import javax.net.ssl.SSLHandshakeException;

public class HTTP {

    private static final String TAG = HTTP.class.getName();

    private HttpURLConnection mConn;
    private OutputStream mOutputStream;
    private int mFilesCount;
    private int mCurrentFileNumber;
    private int mConnectTimeout = 15000;
    private String mStatusText;
    private String mResponseText;
    private short mStatus;

    HTTPResponse request(String method, String url, String contentType, String payload,
                         int connectTimeout, int readTimeout) throws HTTPError {
        connect(url, connectTimeout, readTimeout);
        send(method, contentType, payload);
        readResponse();
        cleanup();
        return new HTTPResponse(mStatus, mStatusText, mResponseText, url);
    }

    private void connect(String endpoint, int connectTimeout, int readTimeout) throws HTTPError {
        try {
            URL url = new URL(endpoint);
            mConn = (HttpURLConnection) url.openConnection();
            mConn.setConnectTimeout(connectTimeout);
            mConn.setReadTimeout(readTimeout);
            mConn.connect();
        } catch (IOException e) {
            if (e instanceof MalformedURLException) {
                throw new HTTPError(HTTPError.INVALID_URL, e);
            } else if (e instanceof ConnectException) {
                if (e.getMessage().contains("ECONNREFUSED")) {
                    throw new HTTPError(HTTPError.CONNECTION_REFUSED, e);
                } else if (e.getMessage().contains("ENETUNREACH")) {
                    throw new HTTPError(HTTPError.NETWORK_UNREACHABLE, e);
                } else if (e.getMessage().contains("ETIMEDOUT")) {
                    throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, e);
                } else {
                    throw new HTTPError(HTTPError.UNKNOWN, e);
                }
            } else if (e instanceof SSLHandshakeException) {
                throw new HTTPError(HTTPError.SSL_CERTIFICATE_INVALID, e);
            } else if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, e);
            }
        }
    }

    private void send(String method, String contentType, Object payload) throws HTTPError {
        try {
            mConn.setRequestMethod(method);
        } catch (ProtocolException e) {
            throw new HTTPError(HTTPError.INVALID_REQUEST_METHOD, e);
        }
        mConn.setRequestProperty("Content-Type", contentType);
        if (payload instanceof FormData) {
            sendRequest((FormData) payload);
        } else {
            sendRequest((String) payload);
        }
    }

    private void sendRequest(String data) throws HTTPError {
        if (data != null) {
            mConn.setFixedLengthStreamingMode(data.getBytes().length);
        }
        if (data != null) {
            sendRequestData(data);
        }
    }

    private void sendRequest(FormData data) throws HTTPError {
        mConn.setFixedLengthStreamingMode(data.getContentLength());
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

    private void cleanup() {
        try {
            mOutputStream.flush();
            mOutputStream.close();
        } catch (IOException ignore) {
            // We are done...
        }
        mConn.disconnect();
    }

    private void readResponse() throws HTTPError {
        assignResponseCodeAndMessage();
        try {
            readFromInputStream(mConn.getInputStream());
        } catch (IOException ignore) {
            readFromInputStream(mConn.getErrorStream());
        }
    }

    private void assignResponseCodeAndMessage() throws HTTPError {
        try {
            Log.v(TAG, "Getting response headers");
            mStatus = (short) mConn.getResponseCode();
            Log.d(TAG, String.format("Request status: %s", mStatus));
            mStatusText = mConn.getResponseMessage();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, e);
            }
        }
    }

    private void readFromInputStream(InputStream inputStream) throws HTTPError {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            mResponseText = output.toString();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, e);
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
            Log.e(TAG, e.getMessage(), e);

            if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, e);
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
//                emitOnFileUploadProgress(uploadFile, uploaded, total);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof FileNotFoundException) {
                if (e.getMessage().contains("ENOENT")) {
                    throw new HTTPError(HTTPError.FILE_DOES_NOT_EXIST, e);
                } else if (e.getMessage().contains("EACCES")) {
                    throw new HTTPError(HTTPError.FILE_READ_PERMISSION_DENIED, e);
                } else {
                    throw new HTTPError(HTTPError.UNKNOWN, e);
                }
            } else if (e instanceof SocketException) {
                throw new HTTPError(HTTPError.LOST_CONNECTION, e);
            } else if (e instanceof SocketTimeoutException) {
                throw new HTTPError(HTTPError.CONNECTION_TIMED_OUT, e);
            } else {
                throw new HTTPError(HTTPError.UNKNOWN, e);
            }
        }
    }
}
