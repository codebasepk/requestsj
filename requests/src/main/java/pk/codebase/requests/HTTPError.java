package pk.codebase.requests;

public class HTTPError extends Exception {

    public static final short STAGE_CONN = 0;
    public static final short STAGE_SEND = 1;
    public static final short STAGE_RECV = 2;

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

    public final short code;

    public HTTPError(short code, Throwable cause) {
        super(cause);
        this.code = code;
    }
}
