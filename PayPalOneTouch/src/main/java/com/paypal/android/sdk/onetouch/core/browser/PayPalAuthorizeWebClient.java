package com.paypal.android.sdk.onetouch.core.browser;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PayPalAuthorizeWebClient extends WebViewClient {

    private final PayPalAuthorizeActivity activity;
    private final String uriScheme;

    public PayPalAuthorizeWebClient(PayPalAuthorizeActivity activity, String uriScheme) {
        this.activity = activity;
        this.uriScheme = uriScheme;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (!url.startsWith(uriScheme)) {
            return false;
        }
        activity.finishWithResult(url);
        return true;
    }
}
