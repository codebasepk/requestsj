package pk.codebase.requests;

public class HTTPOptions {

    private static final int DEFAULT_TIMEOUT = 15000; // 15 seconds

    public final int connectTimeout;
    public final int readTimeout;

    public HTTPOptions(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public HTTPOptions() {
        this.connectTimeout = DEFAULT_TIMEOUT;
        this.readTimeout = DEFAULT_TIMEOUT;
    }

    public HTTPOptions(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = DEFAULT_TIMEOUT;
    }
}