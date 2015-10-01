package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class BraintreeBrowserSwitchActivity extends Activity {

    public static final String LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED = "com.braintreepayments.api.messages.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED";
    public static final String EXTRA_REQUEST_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL";
    public static final String EXTRA_REDIRECT_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL";

    private boolean mShouldCancelOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String url = getIntent().getStringExtra(EXTRA_REQUEST_URL);
        if (url == null) {
            url = getIntent().getStringExtra(EXTRA_REDIRECT_URL);
        }

        if (url != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mShouldCancelOnResume) {
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

        Intent broadcastIntent = new Intent(LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED)
                .putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL, intent.getData());
        BraintreeBroadcastManager.getInstance(this).sendBroadcastSync(broadcastIntent);
        finish();
    }
}
