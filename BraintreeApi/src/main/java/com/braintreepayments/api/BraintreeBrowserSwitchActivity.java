package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Helper Activity that will send a Broadcast {@link Intent} once the browser switch completes. This
 * is a workaround for activities that are singleTop or singleTask.
 */
public class BraintreeBrowserSwitchActivity extends Activity {

    public static final int BROWSER_SWITCH_REQUEST_CODE = 13592;

    public static final String LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED =
            "com.braintreepayments.api.messages.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED";
    public static final String EXTRA_INTENT =
            "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_INTENT";
    public static final String EXTRA_RESULT_CODE =
            "com.braintreepayments.api.messages.EXTRA_RESULT_CODE";
    public static final String EXTRA_REQUEST_CODE =
            "com.braintreepayments.api.messages.EXTRA_REQUEST_CODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data == null || data.getPath().contains("cancel")) {
            broadcastResult(intent, RESULT_CANCELED);
        } else {
            broadcastResult(intent, RESULT_OK);
        }

        finish();
    }

    private void broadcastResult(Intent intent, int resultCode) {
        Intent broadcastIntent = new Intent(LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED)
                .putExtra(EXTRA_REQUEST_CODE, BROWSER_SWITCH_REQUEST_CODE)
                .putExtra(EXTRA_RESULT_CODE, resultCode);

        if (intent != null) {
            broadcastIntent.putExtra(EXTRA_INTENT, intent);
        }

        BraintreeBroadcastManager.getInstance(this).sendBroadcastSync(broadcastIntent);
    }
}
