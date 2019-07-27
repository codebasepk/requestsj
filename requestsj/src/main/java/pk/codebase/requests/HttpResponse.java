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

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class HttpResponse {

    private static final String TAG = HttpResponse.class.getName();

    public final int code;
    public final String reason;
    public final String text;
    public final String url;

    public HttpResponse(int code, String reason, String text, String url) {
        this.code = code;
        this.reason = reason;
        this.text = text;
        this.url = url;
    }

    public JsonNode json() {
        if (text != null) {
            try {
                return new ObjectMapper().readTree(text);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    public <T> T pojo(Class<T> expectedType) {
        if (text != null) {
            try {
                return new ObjectMapper().readValue(text, expectedType);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage(), e);
            }
        }
        return null;
    }
}
