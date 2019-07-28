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

public class HttpOptions {

    private static final int DEFAULT_TIMEOUT = 15000; // 15 seconds

    public final int connectTimeout;
    public final int readTimeout;

    public HttpOptions(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public HttpOptions() {
        this.connectTimeout = DEFAULT_TIMEOUT;
        this.readTimeout = DEFAULT_TIMEOUT;
    }

    public HttpOptions(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = DEFAULT_TIMEOUT;
    }
}
