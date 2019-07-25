package com.byteshaft.requests;

public class HTTPResponse {

    private final int statusCode;
    private final String statusText;
    private final String text;
    private final String url;

    public HTTPResponse(int statusCode, String statusText, String text, String url) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.text = text;
        this.url = url;
    }
}
