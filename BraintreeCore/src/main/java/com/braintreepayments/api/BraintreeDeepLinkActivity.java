package com.braintreepayments.api;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This Activity is used by the SDK Activity to handle deep links on behalf of the merchant
 * It should not be used directly.
 */
public class BraintreeDeepLinkActivity extends AppCompatActivity {

    private final BrowserSwitchClient browserSwitchClient = new BrowserSwitchClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        browserSwitchClient.captureResult(this);
        finish();
    }
}
