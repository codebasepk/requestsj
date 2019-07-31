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

import androidx.appcompat.app.AppCompatActivity;

import pk.codebase.requests.HttpError;
import pk.codebase.requests.HttpRequest;
import pk.codebase.requests.HttpResponse;

public class MainActivity extends AppCompatActivity implements HttpRequest.OnResponseListener,
        HttpRequest.OnErrorListener {

    private final String URL_BASE = "https://httpbin.org";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpRequest request = new HttpRequest(URL_BASE);
        request.setOnResponseListener(this);
        request.setOnErrorListener(this);
        request.get("/get");
    }

    @Override
    public void onError(HttpError error) {
        System.out.println(error.code);
        System.out.println(error.reason);
        System.out.println(error.stage);
        error.printStackTrace();
    }

    @Override
    public void onResponse(HttpResponse response) {
        System.out.println(response.text);
    }
}
