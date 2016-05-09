package com.paypal.android.sdk.onetouch.core.browser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

@SuppressLint("SetJavaScriptEnabled")
public class PayPalAuthorizeWebView extends WebView {

    public PayPalAuthorizeWebView(Context context) {
        super(context);
    }

    public PayPalAuthorizeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PayPalAuthorizeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public PayPalAuthorizeWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(PayPalAuthorizeActivity activity, String uriScheme) {
        setId(android.R.id.widget_frame);

        WebSettings settings = getSettings();
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        setWebViewClient(new PayPalAuthorizeWebClient(activity, uriScheme));
    }

}
