package com.byteshaft.requests.sample;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.byteshaft.requests.FormData;
import com.byteshaft.requests.HttpRequest;

import java.io.File;
import java.net.HttpURLConnection;

public class MainActivity extends AppCompatActivity implements
        HttpRequest.OnFileUploadProgressListener, HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener {

    private final String TEST_URL = "http://192.168.1.5:8000/api/me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpRequest request = new HttpRequest(getApplicationContext());
        request.setOnReadyStateChangeListener(this);
        request.setOnFileUploadProgressListener(this);
        request.setOnErrorListener(this);
        request.open("PUT", TEST_URL);
        request.setRequestHeader("Authorization", "Token fd2864175d949c7a01a8d186d751658fb5288581");
        FormData data = new FormData();
        data.append(FormData.TYPE_CONTENT_TEXT, "full_name", " īñ4ëì");
        request.send(data);
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

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_CREATED:
                        Toast.makeText(
                                getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                        break;
                }
        }
    }

    @Override
    public void onFileUploadProgress(HttpRequest request, File file, long loaded, long total) {
        if (request.getCurrentFileNumber() == request.getTotalFiles() && loaded == total) {
            Toast.makeText(getApplicationContext(), "Upload Done", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(HttpRequest request, int readyState, short error, Exception exception) {

    }
}
