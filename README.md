# RequestsJ
Requests for Android

# Getting started
The build is hosted on jcenter so you could just add the line:

```
implementation 'pk.codebase.requestsj:requestsj:0.8.0'
```

# Usage
```java
public class MainActivity extends AppCompatActivity implements HttpRequest.OnResponseListener,
        HttpRequest.OnErrorListener {

    private final String URL_GET = "https://httpbin.org/get";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpRequest request = new HttpRequest();
        request.setOnResponseListener(this);
        request.setOnErrorListener(this);
        request.get(URL_GET);
    }

    private void onResponse(HttpResponse response) {
        if (response.code == HttpRequest.HTTP_OK) {
            System.out.println(response.json());
        }
    }

    private void onError(HttpError error) {
        if (error.code == HttpError.NETWORK_UNREACHABLE) {
            Toast.makeText(this, "Internet Not Available!", Toast.LENGTH_SHORT).show();
        }
    }
}
```
