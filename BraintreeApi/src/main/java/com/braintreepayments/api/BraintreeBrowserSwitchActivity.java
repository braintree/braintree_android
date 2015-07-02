package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BraintreeBrowserSwitchActivity extends Activity {
    public static final String X_CANCEL_HOST = "cancel";
    public static final String X_SUCCESS_HOST = "success";


    public static final String LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED =
            "com.braintreepayments.api.messages.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED";
    public static final String EXTRA_INTENT =
            "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_INTENT";
    public static final String EXTRA_RESULT_CODE =
            "com.braintreepayments.api.messages.EXTRA_RESULT_CODE";

    private static final String TAG = BraintreeBrowserSwitchActivity.class.getSimpleName();

    // Indicates whether or not this Activity has received onPause(), which indicates that
    // it should automatically cancel, if the Activity resumes without a browser switch result.
    private boolean mPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Intent browserIntent = getIntent().getParcelableExtra(EXTRA_INTENT);

        startActivity(browserIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPaused) {
            Log.d(TAG, "Resumed without a response. Canceling.");

            broadcastResult(null, Activity.RESULT_CANCELED);

            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPaused = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(TAG, "Received an intent: " + intent);

        broadcastResult(intent, Activity.RESULT_OK);
        finish();
    }

    private void broadcastResult(Intent intent, int result) {
        Intent broadcastIntent = new Intent(LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED)
                .putExtra(EXTRA_RESULT_CODE, result);

        if (intent != null) {
            broadcastIntent.putExtra(EXTRA_INTENT, intent);
        }

        BraintreeBroadcastManager.getInstance(this).sendBroadcastSync(broadcastIntent);
    }
}
