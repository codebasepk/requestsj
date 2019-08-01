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

public class HttpError extends Exception {

    public static final short STAGE_UNKNOWN = 0;
    public static final short STAGE_CONNECTING = 1;
    public static final short STAGE_SENDING = 2;
    public static final short STAGE_RECEIVING = 3;
    public static final short STAGE_CLEANING = 4;

    public static final short UNKNOWN = 0;
    public static final short INVALID_URL = 1;
    public static final short INVALID_REQUEST_METHOD = 2;
    public static final short CONNECTION_REFUSED = 3;
    public static final short SSL_CERTIFICATE_INVALID = 4;
    public static final short FILE_DOES_NOT_EXIST = 5;
    public static final short FILE_READ_PERMISSION_DENIED = 6;
    public static final short NETWORK_UNREACHABLE = 7;
    public static final short CONNECTION_TIMED_OUT = 8;
    public static final short LOST_CONNECTION = 9;
    public static final short CANNOT_SERIALIZE = 10;

    public short code = UNKNOWN;
    public final short stage;
    public final String reason;

    public HttpError(short stage, Throwable cause) {
        super(cause);
        this.stage = stage;
        this.reason = cause.getMessage();
    }

    public HttpError(short code, short stage, Throwable cause) {
        super(cause);
        this.code = code;
        this.stage = stage;
        this.reason = cause.getMessage();
    }

    void setCode(short code) {
        this.code = code;
    }
}
