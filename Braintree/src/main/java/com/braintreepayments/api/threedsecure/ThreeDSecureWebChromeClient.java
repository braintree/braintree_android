package com.braintreepayments.api.threedsecure;

import android.os.Message;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

@Deprecated
public class ThreeDSecureWebChromeClient extends WebChromeClient {

    private ThreeDSecureWebViewActivity mActivity;

    public ThreeDSecureWebChromeClient(ThreeDSecureWebViewActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        ThreeDSecureWebView newWebView = new ThreeDSecureWebView(mActivity.getApplicationContext());
        newWebView.init(mActivity);
        mActivity.pushNewWebView(newWebView);
        ((WebView.WebViewTransport) resultMsg.obj).setWebView(newWebView);
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
}
