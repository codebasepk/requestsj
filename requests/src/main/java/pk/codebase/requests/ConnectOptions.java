package pk.codebase.requests;

public class ConnectOptions {

    private static final int DEFAULT_TIMEOUT = 15000; // 15 seconds

    public final int connectTimeout;
    public final int readTimeout;

    public ConnectOptions(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public ConnectOptions() {
        this.connectTimeout = DEFAULT_TIMEOUT;
        this.readTimeout = DEFAULT_TIMEOUT;
    }

    public ConnectOptions(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = DEFAULT_TIMEOUT;
    }
}
