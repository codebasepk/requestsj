package pk.codebase.requests.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import pk.codebase.requests.HTTPError;
import pk.codebase.requests.HTTPRequest;
import pk.codebase.requests.HTTPResponse;

public class MainActivity extends AppCompatActivity {

    private final String TEST_URL = "https://httpbin.org/post";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HTTPRequest request = new HTTPRequest();
        request.setOnResponseListener(new HTTPRequest.OnResponseListener() {
            @Override
            public void onResponse(HTTPResponse response) {
                System.out.println(response.statusCode);
                System.out.println(response.statusText);
                try {
                    JsonNode node = response.json();
                    System.out.println(node);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        request.setOnErrorListener(new HTTPRequest.OnErrorListener() {
            @Override
            public void onError(HTTPError error) {
                System.out.println(error.code);
                error.printStackTrace();
            }
        });
        request.get("https://httpbin.org/get");
    }
}
