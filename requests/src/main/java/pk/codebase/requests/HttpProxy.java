package pk.codebase.requests;

public class HttpProxy {

    private String host;
    private String port;
    private String username;
    private String password;

    public HttpProxy(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public HttpProxy(String host, String port, String username, String password) {
        this(host, port);
        this.username = username;
        this.password = password;

    }
}
