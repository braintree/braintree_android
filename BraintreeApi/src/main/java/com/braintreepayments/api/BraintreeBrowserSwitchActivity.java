package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class BraintreeBrowserSwitchActivity extends Activity {

    public static final String EXTRA_REQUEST_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL";
    public static final String EXTRA_REDIRECT_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL";
    public static final String LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED = "com.braintreepayments.api.messages.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED";
    public static final String BROADCAST_BROWSER_EXTRA_RESULT = "com.braintreepayments.api.messages.BROADCAST_BROWSER_EXTRA_RESULT";

    private boolean mShouldCancelOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String stringExtra = getIntent().getStringExtra(EXTRA_REQUEST_URL);
        if (stringExtra == null) {
            stringExtra = getIntent().getStringExtra(EXTRA_REDIRECT_URL);
        }

        if (stringExtra != null) {
            Uri uri = Uri.parse(stringExtra);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }

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
        //Doesn't work in API 19
//        Intent responseIntent = new Intent();
//        responseIntent.putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL, intent.getData());
//        setResult(Activity.RESULT_OK, responseIntent);

        Uri redirectUri = intent.getData();
        String error = redirectUri.getQueryParameter("error");
        Intent broadcastIntent = new Intent(LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED);
        broadcastIntent.putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL,
                intent.getData());
        if (error != null) {
            broadcastIntent.putExtra(BROADCAST_BROWSER_EXTRA_RESULT, Activity.RESULT_CANCELED);
        } else {
            broadcastIntent.putExtra(BROADCAST_BROWSER_EXTRA_RESULT, Activity.RESULT_OK);
        }
        BraintreeBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        finish();
    }
}
