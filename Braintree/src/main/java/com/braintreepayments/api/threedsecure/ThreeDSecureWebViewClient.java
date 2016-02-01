package com.braintreepayments.api.threedsecure;

import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;

public class ThreeDSecureWebViewClient extends WebViewClient {

    private ThreeDSecureWebViewActivity mActivity;

    public ThreeDSecureWebViewClient(ThreeDSecureWebViewActivity activity) {
        mActivity = activity;
    }

    public void onPageStarted(WebView view, String url, Bitmap icon) {
        if (url.contains("html/authentication_complete_frame")) {
            view.stopLoading();

            String authResponseJson = Uri.parse(url).getQueryParameter("auth_response");
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
}
