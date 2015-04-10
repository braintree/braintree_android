package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class BraintreeBrowserSwitchActivity extends Activity {

    public static final String EXTRA_REQUEST_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL";
    public static final String EXTRA_REDIRECT_URL = "com.braintreepayments.api.BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL";

    private boolean mShouldCancelOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //String stringExtra = "https://www.coinbase.com/oauth/authorize?response_type=code&client_id=11d27229ba58b56d7e3c01a0527f4d5b446d4f684817cb623d255b573addc59b&scope=authorizations%3Abraintree+user&redirect_uri=com.braintreepayments.demo.braintree%3A%2F%2Fcoinbase&meta%5Bauthorizations_merchant_account%5D=coinbase-development-merchant%40getbraintree.com";

        String stringExtra = getIntent().getStringExtra(EXTRA_REQUEST_URL);
        if (stringExtra == null) {
            stringExtra = getIntent().getStringExtra(EXTRA_REDIRECT_URL);





        }

        if (stringExtra != null) {

            Uri uri = Uri.parse(stringExtra);
            Log.d("BrowserSwitchActi//vity", "uri=" + uri);





//        Intent responseIntent = new Intent();
//        responseIntent.putExtra(BraintreeBrowserSwitchActivity.EXTRA_REDIRECT_URL, "2722f4b213b9d14886839a14746fc404a1e45404e5dd84ba0aeb0403db8d4362");
//        setResult(Activity.RESULT_OK, responseIntent);
//
//        finish();
            intent.setData(uri);


        }

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
