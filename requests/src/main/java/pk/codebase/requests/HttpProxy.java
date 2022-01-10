package pk.codebase.requests;

public class HttpProxy {

    private String host;
    private int port;
    private String username;
    private String password;



    public HttpProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public HttpProxy(String host, int port, String username, String password) {
        this(host, port);
        this.username = username;
        this.password = password;

    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }
}
