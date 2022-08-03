package com.braintreepayments.api;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BraintreeDeepLinkActivity extends AppCompatActivity {

    BrowserSwitchClient browserSwitchClient = new BrowserSwitchClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        browserSwitchClient.captureResult(this);
        finish();
    }
}
