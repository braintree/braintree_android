package com.braintreepayments.api;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This Activity is used by the SDK Activity to enable default deep link handling and should not
 * be used directly.
 */
public class BraintreeDeepLinkActivity extends AppCompatActivity {

    BrowserSwitchClient browserSwitchClient = new BrowserSwitchClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        browserSwitchClient.captureResult(this);
        finish();
    }
}
