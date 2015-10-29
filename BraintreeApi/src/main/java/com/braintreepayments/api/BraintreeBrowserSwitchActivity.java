package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Helper Activity that captures the response when browser switch completes.
 */
public class BraintreeBrowserSwitchActivity extends Activity {

    static final String EXTRA_BROWSER_SWITCH = "com.braintreepayments.api.BROWSER_SWITCH";

    static int sLastBrowswerSwitchResultCode;
    static Intent sLastBrowserSwitchResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        sLastBrowserSwitchResponse = getIntent();

        if (getIntent() == null || getIntent().getData() == null ||
                getIntent().getData().getPath().contains("cancel")) {
            sLastBrowswerSwitchResultCode = RESULT_CANCELED;
        } else {
            sLastBrowswerSwitchResultCode = RESULT_OK;
        }

        finish();
    }
}
