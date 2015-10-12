package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Helper Activity that will send a Broadcast {@link Intent} once the browser switch completes.
 * This is a workaround for activities that are singleTop or singleTask.
 *
 * Steps that this Activity takes are:
 * <ol>
 *     <li>Launches an {@link Intent} to open the browser.</li>
 *     <li>Waits for browser to complete.</li>
 *     <li>{@link #onPause()} is called when the browser is complete.</li>
 *     <li>{@link #onResume()} is then called, which allows us to send the switch complete broadcast.</li>
 *     <li>{@link #finish()} the activity.</li>
 * </ol>
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

    /**
     * Indicates whether or not this Activity has received onPause(), which indicates that
     * it should automatically cancel, if the Activity resumes without a browser switch result.
     */
    private boolean mPaused = false;

    private int mRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mRequestCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, BROWSER_SWITCH_REQUEST_CODE);
        startActivity((Intent) getIntent().getParcelableExtra(EXTRA_INTENT));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPaused) {
            broadcastResult(new Intent(), Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPaused = true;
    }

    /**
     * Overridden for testing
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        onNewIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getDataString() == null || intent.getDataString().contains("cancel")) {
            broadcastResult(intent, RESULT_CANCELED);
        } else {
            broadcastResult(intent, RESULT_OK);
        }

        finish();
    }

    private void broadcastResult(Intent intent, int resultCode) {
        Intent broadcastIntent = new Intent(LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED)
                .putExtra(EXTRA_REQUEST_CODE, mRequestCode)
                .putExtra(EXTRA_RESULT_CODE, resultCode);

        if (intent != null) {
            broadcastIntent.putExtra(EXTRA_INTENT, intent);
        }

        BraintreeBroadcastManager.getInstance(this).sendBroadcastSync(broadcastIntent);
    }
}
