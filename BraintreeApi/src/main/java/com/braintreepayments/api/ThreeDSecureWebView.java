package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;

@SuppressLint("SetJavaScriptEnabled")
public class ThreeDSecureWebView extends WebView {

    private ThreeDSecureWebViewActivity mActivity;

    public ThreeDSecureWebView(Context context) {
        super(context);
        init();
    }

    public ThreeDSecureWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyleAttr,
            boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public ThreeDSecureWebView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setId(android.R.id.widget_frame);

        WebSettings settings = getSettings();
        settings.setUserAgentString(HttpRequest.USER_AGENT);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        disableOnScreenZoomControls(settings);

        setWebChromeClient(mThreeDSecureWebChromeClient);
        setWebViewClient(mThreeDSecureWebViewClient);
    }

    protected void setActivity(ThreeDSecureWebViewActivity activity) {
        mActivity = activity;
    }

    private WebViewClient mThreeDSecureWebViewClient = new WebViewClient() {
        public void onPageStarted(WebView view, String url, Bitmap icon) {
            if (url.contains("html/authentication_complete_frame")) {
                view.stopLoading();

                String authResponseJson = (Uri.parse(url).getQueryParameter("auth_response"));
                mActivity.finishWithResult(
                        ThreeDSecureAuthenticationResponse.fromJson(authResponseJson));
            } else {
                super.onPageStarted(view, url, icon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mActivity.setActionBarTitle(view.getTitle());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            view.stopLoading();
            mActivity.finishWithResult(
                    ThreeDSecureAuthenticationResponse.fromException(description));
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            view.stopLoading();
            mActivity.finishWithResult(
                    ThreeDSecureAuthenticationResponse.fromException(error.toString()));
        }
    };

    private WebChromeClient mThreeDSecureWebChromeClient = new WebChromeClient() {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                Message resultMsg) {
            ThreeDSecureWebView newWebView = new ThreeDSecureWebView(getContext());
            newWebView.setActivity(mActivity);
            mActivity.pushNewWebView(newWebView);
            ((WebView.WebViewTransport) resultMsg.obj)
                    .setWebView(newWebView);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            mActivity.popCurrentWebView();
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress < 100) {
                mActivity.setProgress(newProgress);
                mActivity.setProgressBarVisibility(true);
            } else {
                mActivity.setProgressBarVisibility(false);
            }
        }
    };

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void disableOnScreenZoomControls(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.setDisplayZoomControls(false);
        }
    }
}
