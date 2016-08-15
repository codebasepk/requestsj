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

import java.io.File;
import java.util.ArrayList;

public class FormData {

    private static final String CHARSET = "UTF-8";
    private static final String NEW_LINE = "\r\n";
    private static final String DASHES = "--";
    private static final String BOUNDARY_LINE = "---------------------------";
    private static final String SEMICOLON = "; ";
    private ArrayList<MultiPartData> mData;
    private int mContentLength = FINISH_LINE.length();

    public static final String BOUNDARY = BOUNDARY_LINE + System.currentTimeMillis();
    public static final int TYPE_CONTENT_TEXT = 1;
    public static final int TYPE_CONTENT_FILE = 2;
    public static String FINISH_LINE = String.format(
            "%s%s%s%s", DASHES, BOUNDARY, DASHES, NEW_LINE
    );

    public FormData() {
        mData = new ArrayList<>();
    }

    private String getContentTypeString(int contentType) {
        switch (contentType) {
            case TYPE_CONTENT_TEXT:
                return String.format("Content-Type: text/plain; charset=%s", CHARSET);
            case TYPE_CONTENT_FILE:
                return "Content-Type: Content-Transfer-Encoding: binary";
            default:
                throw new UnsupportedOperationException("Invalid content type.");
        }
    }

    private String getFieldDispositionLine(int fieldType, String fieldName, String fileName) {
        String simpleDispositionLine = String.format(
                "Content-Disposition: form-data; name=\"%s\"", fieldName
        );
        switch (fieldType) {
            case TYPE_CONTENT_TEXT:
                return simpleDispositionLine;
            case TYPE_CONTENT_FILE:
                String fileNameLine = String.format("filename=\"%s\"", fileName);
                return simpleDispositionLine + SEMICOLON + fileNameLine;
            default:
                throw new UnsupportedOperationException("Invalid content type.");
        }
    }

    private String getFieldPreContentWriteString(int contentType, String fieldName, String value) {
        return DASHES
                + BOUNDARY
                + NEW_LINE
                + getFieldDispositionLine(contentType, fieldName, value)
                + NEW_LINE
                + getContentTypeString(contentType)
                + NEW_LINE
                + NEW_LINE;
    }

    private String getFieldPostContentWriteString(int contentType) {
        switch (contentType) {
            case TYPE_CONTENT_TEXT:
                return NEW_LINE;
            case TYPE_CONTENT_FILE:
                return NEW_LINE + NEW_LINE;
            default:
                throw new UnsupportedOperationException("Invalid content type.");
        }
    }

    public void append(int contentType, String fieldName, String value) {
        MultiPartData data = new MultiPartData();
        data.setContentType(contentType);
        String preContentString = getFieldPreContentWriteString(contentType, fieldName, value);
        mContentLength += preContentString.length();
        data.setPreContentData(preContentString);
        String postContentString = getFieldPostContentWriteString(contentType);
        if (contentType == TYPE_CONTENT_TEXT) {
            mContentLength += value.length();
            mContentLength += postContentString.length();
        } else if (contentType == TYPE_CONTENT_FILE) {
            File file = new File(value);
            mContentLength += file.length();
            mContentLength += postContentString.length();
        }
        data.setContent(value);
        data.setPostContentData(postContentString);
        mData.add(data);
    }

    public int getContentLength() {
        return mContentLength;
    }

    public ArrayList<MultiPartData> getData() {
        return mData;
    }

    public class MultiPartData {
        private int mContentType;
        private String mPreContentData;
        private String mContent;
        private String mPostContentData;

        public void setContentType(int contentType) {
            mContentType = contentType;
        }

        public int getContentType() {
            return mContentType;
        }

        public void setPreContentData(String preContentData) {
            mPreContentData = preContentData;
        }

        public String getPreContentData() {
            return mPreContentData;
        }

        public void setPostContentData(String postContentData) {
            mPostContentData = postContentData;
        }

        public String getPostContentData() {
            return mPostContentData;
        }

        public void setContent(String content) {
            mContent = content;
        }

        public String getContent() {
            return mContent;
        }
    }
}
