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

package pk.codebase.requests.sample;

import android.os.Bundle;
import android.provider.FontRequest;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pk.codebase.requests.FormData;
import pk.codebase.requests.HTTPError;
import pk.codebase.requests.HTTPRequest;
import pk.codebase.requests.HTTPResponse;

public class MainActivity extends AppCompatActivity implements HTTPRequest.OnResponseListener,
        HTTPRequest.OnErrorListener {

    private final String URL_GET = "https://httpbin.org/get";
    private final String URL_POST = "https://httpbin.org/post";
    private final String URL_PUT = "https://httpbin.org/put";
    private final String URL_DELETE = "https://httpbin.org/delete";
    private final String URL_PATCH = "https://httpbin.org/patch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HTTPRequest request = new HTTPRequest();
        request.setOnResponseListener(this);
        request.setOnErrorListener(this);
        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Token asdasdasdsa");
//        request.get(URL_GET, headers);
        FormData data = new FormData();
        data.append(FormData.TYPE_CONTENT_TEXT, "name", "omer");
        request.post(URL_POST, data, headers);
    }

    @Override
    public void onError(HTTPError error) {
        System.out.println(error.code);
        System.out.println(error.reason);
        System.out.println(error.stage);
        error.printStackTrace();
    }

    @Override
    public void onResponse(HTTPResponse response) {
        System.out.println(response.code);
        System.out.println(response.reason);
        System.out.println(response.text);
//        System.out.println(response.pojo(User.class));
    }

    static class User {
        private String firstName;
        private String lastName;

        public User(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }
}
