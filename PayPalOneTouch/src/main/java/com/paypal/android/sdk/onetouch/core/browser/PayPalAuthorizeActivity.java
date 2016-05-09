package com.paypal.android.sdk.onetouch.core.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;

public class PayPalAuthorizeActivity extends Activity {

    public static final String REDIRECT_URI_SCHEME_ARG = "PayPalAuthorizeActivity.RedirectUri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootView = (FrameLayout) findViewById(android.R.id.content);

        String uriScheme = getIntent().getStringExtra(REDIRECT_URI_SCHEME_ARG);

        PayPalAuthorizeWebView payPalAuthorizeWebView = new PayPalAuthorizeWebView(this);
        payPalAuthorizeWebView.init(this, uriScheme);
        payPalAuthorizeWebView.loadUrl(getIntent().getData().toString());

        rootView.addView(payPalAuthorizeWebView);
    }

    public void finishWithResult(String url) {
        setResult(Activity.RESULT_OK,  new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        finish();
    }
}
