package com.byteshaft.requests.sample;

import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;

import com.byteshaft.requests.FormData;
import com.byteshaft.requests.HTTPRequest;

public class MainActivity extends AppCompatActivity {

    private final String TEST_URL = "https://httpbin.org/post";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HTTPRequest request = new HTTPRequest();
        request.setOnResponseListener((response, status) -> {
            System.out.println(response.getResponseText());
        });
        request.setOnFileUploadProgressListener((file, loaded, total) -> {

        });
        request.setOnErrorListener((response, error, exception) -> {
            System.out.println("WE have the error " + response.toString());
        });
        request.setRequestHeader("Authorization", "Token fd2864175d949c7a01a8d186d751658fb5288581");
        FormData data = new FormData();
        data.append(FormData.TYPE_CONTENT_TEXT, "full_name", " īñ4ëì");
        request.send("POST", TEST_URL, data);
    }

    private FormData getData() {
        FormData data = new FormData();
        data.append(FormData.TYPE_CONTENT_TEXT, "email", "x@gmail.com");
        data.append(FormData.TYPE_CONTENT_TEXT, "driving_experience", "NEW");
        data.append(FormData.TYPE_CONTENT_TEXT, "full_name", "X User");
        data.append(FormData.TYPE_CONTENT_TEXT, "password", "x11");
        data.append(FormData.TYPE_CONTENT_TEXT, "phone_number", "911");
        data.append(FormData.TYPE_CONTENT_TEXT, "transmission_type", "0");
        data.append(FormData.TYPE_CONTENT_FILE, "doc1", getPath());
        data.append(FormData.TYPE_CONTENT_FILE, "doc2", getPath());
        data.append(FormData.TYPE_CONTENT_FILE, "doc3", getPath());
        return data;
    }

    private String getPath() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        return dirPath + "/ok.png";
    }
}
