package com.byteshaft.requests.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.byteshaft.requests.HttpRequest;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpRequest r = new HttpRequest(getApplicationContext());
    }
}
