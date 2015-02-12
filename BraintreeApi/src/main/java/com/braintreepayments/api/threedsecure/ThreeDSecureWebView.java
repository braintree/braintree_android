package com.braintreepayments.api.threedsecure;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.internal.HttpRequest;

@Beta
@SuppressLint("SetJavaScriptEnabled")
public class ThreeDSecureWebView extends WebView {

    public ThreeDSecureWebView(Context context) {
        super(context);
    }

    public ThreeDSecureWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyleAttr,
            boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(ThreeDSecureWebViewActivity activity) {
        setId(android.R.id.widget_frame);

        WebSettings settings = getSettings();
        settings.setUserAgentString(HttpRequest.USER_AGENT);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        disableOnScreenZoomControls(settings);

        setWebChromeClient(new ThreeDSecureWebChromeClient(activity));
        setWebViewClient(new ThreeDSecureWebViewClient(activity));
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void disableOnScreenZoomControls(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.setDisplayZoomControls(false);
        }
    }
}
