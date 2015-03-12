package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class BraintreeBrowserSwitchActivity extends Activity {

    public static final String EXTRA_REQUEST_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL";
    private static final String EXTRA_REDIRECT_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL";

    private boolean mShouldCancelOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getIntent().getStringExtra(EXTRA_REQUEST_URL)));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mShouldCancelOnResume) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mShouldCancelOnResume = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Intent responseIntent = new Intent();
        responseIntent.putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL, intent.getData());
        setResult(Activity.RESULT_OK, responseIntent);
        finish();
    }
}
