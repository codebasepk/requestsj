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
    private static final String CRLF = "\r\n";
    private static final String DASHES = "--";
    private static final String BOUNDARY_LINE = "---------------------------";
    private static final String SEMICOLON = "; ";
    private final ArrayList<MultiPartData> mData;
    private int mContentLength = FINISH_LINE.length();
    private int mFilesCount;

    static final String BOUNDARY = BOUNDARY_LINE + System.currentTimeMillis();
    public static final String FINISH_LINE = String.format("%s%s%s%s", DASHES, BOUNDARY, DASHES, CRLF);

    public static final int TYPE_CONTENT_TEXT = 1;
    public static final int TYPE_CONTENT_FILE = 2;

    public FormData() {
        mData = new ArrayList<>();
    }

    private String getContentTypeString(int contentType) {
        if (contentType == TYPE_CONTENT_TEXT) {
            return String.format("Content-Type: text/plain; charset=%s", CHARSET);
        } else {
            return "Content-Type: Content-Transfer-Encoding: binary";
        }
    }

    private String getFieldDispositionLine(int fieldType, String fieldName, String fileName) {
        String simpleDispositionLine = String.format(
                "Content-Disposition: form-data; name=\"%s\"", fieldName
        );
        if (fieldType == TYPE_CONTENT_TEXT) {
            return simpleDispositionLine;
        } else {
            String fileNameLine = String.format("filename=\"%s\"", fileName);
            return simpleDispositionLine + SEMICOLON + fileNameLine;
        }
    }

    private String getFieldPreContentWriteString(int contentType, String fieldName, String value) {
        return DASHES
                + BOUNDARY
                + CRLF
                + getFieldDispositionLine(contentType, fieldName, value)
                + CRLF
                + getContentTypeString(contentType)
                + CRLF
                + CRLF;
    }

    private String getFieldPostContentWriteString(int contentType) {
        if (contentType == TYPE_CONTENT_TEXT) {
            return CRLF;
        } else {
            return CRLF + CRLF;
        }
    }

    public void append(int contentType, String fieldName, String value) {
        if (contentType != TYPE_CONTENT_FILE && contentType != TYPE_CONTENT_TEXT) {
            throw new IllegalArgumentException("Invalid content type.");
        }
        MultiPartData data = new MultiPartData();
        data.setContentType(contentType);
        String preContentString = getFieldPreContentWriteString(contentType, fieldName, value);
        mContentLength += preContentString.length();
        data.setPreContentData(preContentString);
        if (contentType == TYPE_CONTENT_TEXT) {
            mContentLength += value.getBytes().length;
        } else {
            File file = new File(value);
            mContentLength += file.length();
            mFilesCount += 1;
        }
        data.setContent(value);
        String postContentString = getFieldPostContentWriteString(contentType);
        mContentLength += postContentString.length();
        data.setPostContentData(postContentString);
        mData.add(data);
    }

    int getContentLength() {
        return mContentLength;
    }

    int getFilesCount() {
        return mFilesCount;
    }

    ArrayList<MultiPartData> getData() {
        return mData;
    }

    class MultiPartData {
        private int mContentType;
        private String mPreContentData;
        private String mContent;
        private String mPostContentData;

        private void setContentType(int contentType) {
            mContentType = contentType;
        }

        int getContentType() {
            return mContentType;
        }

        private void setPreContentData(String preContentData) {
            mPreContentData = preContentData;
        }

        String getPreContentData() {
            return mPreContentData;
        }

        private void setPostContentData(String postContentData) {
            mPostContentData = postContentData;
        }

        String getPostContentData() {
            return mPostContentData;
        }

        private void setContent(String content) {
            mContent = content;
        }

        String getContent() {
            return mContent;
        }
    }
}
