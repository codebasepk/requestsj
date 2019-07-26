package com.byteshaft.requests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class HTTPResponse {

    public final int statusCode;
    public final String statusText;
    public final String text;
    public final String url;

    public HTTPResponse(int statusCode, String statusText, String text, String url) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.text = text;
        this.url = url;
    }

    public JsonNode json() throws IOException {
        if (text == null) {
            return null;
        } else {
            return new ObjectMapper().readTree(text);
        }
    }
}
