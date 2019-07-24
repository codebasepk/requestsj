package com.byteshaft.requests;

public class HttpResponse {
    private final int mFilesCount;
    private final int mCurrentFileNumber;
    private final int mConnectTimeout;
    private final int mStatus;
    private final String mStatusText;
    private final String mResponseText;
    private final String mUrl;

    public HttpResponse(int filesCount, int currentFileNumber, int connectTimeout, int status,
                        String statusText, String responseText, String url) {
        mFilesCount = filesCount;
        mCurrentFileNumber = currentFileNumber;
        mConnectTimeout = connectTimeout;
        mStatus = status;
        mStatusText = statusText;
        mResponseText = responseText;
        mUrl = url;
    }

    public int getFilesCount() {
        return mFilesCount;
    }

    public int getCurrentFileNumber() {
        return mCurrentFileNumber;
    }

    public int getConnectTimeout() {
        return mConnectTimeout;
    }

    public int getStatus() {
        return mStatus;
    }

    public String getStatusText() {
        return mStatusText;
    }

    public String getResponseText() {
        return mResponseText;
    }

    public String getResponse() {
        return mUrl;
    }
}
