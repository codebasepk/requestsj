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
