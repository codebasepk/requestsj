package com.byteshaft.requests.sample;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import com.byteshaft.requests.FormData;
import com.byteshaft.requests.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

public class MainActivity extends AppCompatActivity implements
        HttpRequest.OnFileUploadProgressListener, HttpRequest.OnReadyStateChangeListener {

    private final String TEST_URL = "http://localhost:8000/api/user/driver-registration";
    private HttpRequest mRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRequest = new HttpRequest(getApplicationContext());
        mRequest.setOnReadyStateChangeListener(this);
        mRequest.setOnFileUploadProgressListener(this);
        mRequest.open("POST", TEST_URL);
        mRequest.send(getData());
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
        return dirPath + "/ok2.png";
    }

    @Override
    public void onFileUploadProgress(File file, long uploaded, long total) {
        System.out.println(uploaded);
    }

    @Override
    public void onReadyStateChange(HttpURLConnection connection, int readyState) {
        if (readyState == HttpRequest.STATE_DONE) {
            try {
                System.out.println(connection.getResponseMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(mRequest.getResponseText());
        }
    }
}
