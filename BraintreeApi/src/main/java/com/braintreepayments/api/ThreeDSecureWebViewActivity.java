package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ThreeDSecureWebViewActivity extends Activity {

    public static final String EXTRA_THREE_D_SECURE_LOOKUP = "com.braintreepayments.api.EXTRA_THREE_D_SECURE_LOOKUP";
    public static final String EXTRA_THREE_D_SECURE_RESULT = "com.braintreepayments.api.EXTRA_THREE_D_SECURE_RESULT";

    private static final String CLOSE_URL_SCHEME = "close-page://";
    private static final String JAVASCRIPT_MODIFICATION = "javascript:(function() {" +
            "var as = document.getElementsByTagName('a');" +
            "for (var i = 0; i < as.length; i++) {" +
            "  if (as[i]['href'] === 'javascript:window.close();') {" +
            "    as[i]['href'] = '" + CLOSE_URL_SCHEME + "';" +
            "  };" +
            "};" +
            "var forms = document.getElementsByTagName('form');" +
            "for (var i = 0; i < forms.length; i++) {" +
            "  if (forms[i]['action'].indexOf('close_window.htm') > -1) {" +
            "    forms[i]['action'] = '" + CLOSE_URL_SCHEME + "';" +
            "  };" +
            "};" +
            "})()";

    private ActionBar mActionBar;
    private WebView mThreeDSecureWebView;
    private ThreeDSecureLookup mThreeDSecureLookup;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        mThreeDSecureLookup = getIntent().getParcelableExtra(EXTRA_THREE_D_SECURE_LOOKUP);
        if (mThreeDSecureLookup == null) {
            throw new IllegalArgumentException("A ThreeDSecureLookup must be specified with " +
                    ThreeDSecureLookup.class.getSimpleName() + ".EXTRA_THREE_D_SECURE_LOOKUP extra");
        }

        setupActionBar();

        mThreeDSecureWebView = new WebView(this);
        mThreeDSecureWebView.setId(android.R.id.widget_frame);
        mThreeDSecureWebView.setWebChromeClient(mThreeDSecureWebChromeClient);
        mThreeDSecureWebView.setWebViewClient(mThreeDSecureWebViewClient);
        mThreeDSecureWebView.getSettings().setUserAgentString(HttpRequest.USER_AGENT);
        mThreeDSecureWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mThreeDSecureWebView.getSettings().setJavaScriptEnabled(true);
        mThreeDSecureWebView.getSettings().setBuiltInZoomControls(true);

        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            mThreeDSecureWebView.getSettings().setDisplayZoomControls(false);
        }

        ((FrameLayout) findViewById(android.R.id.content)).addView(mThreeDSecureWebView);

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("PaReq", mThreeDSecureLookup.getPareq()));
        params.add(new BasicNameValuePair("MD", mThreeDSecureLookup.getMd()));
        params.add(new BasicNameValuePair("TermUrl", mThreeDSecureLookup.getTermUrl()));
        ByteArrayOutputStream encodedParams = new ByteArrayOutputStream();
        try {
            new UrlEncodedFormEntity(params, HTTP.UTF_8).writeTo(encodedParams);
        } catch (IOException e) {
            finish();
        }
        mThreeDSecureWebView.postUrl(mThreeDSecureLookup.getAcsUrl(), encodedParams.toByteArray());
    }

    private WebViewClient mThreeDSecureWebViewClient = new WebViewClient() {
        public void onPageStarted(WebView view, String url, Bitmap icon) {
            if (url.equals(CLOSE_URL_SCHEME) && view.canGoBack()) {
                view.stopLoading();
                view.goBack();
            } else if (url.contains("html/authentication_complete_frame")) {
                view.stopLoading();

                String authResponseJson = (Uri.parse(url).getQueryParameter("auth_response"));
                finishWithResult(ThreeDSecureAuthenticationResponse.fromJson(authResponseJson));
            } else {
                super.onPageStarted(view, url, icon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setActionBarTitle(view.getTitle());
            view.loadUrl(JAVASCRIPT_MODIFICATION);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            view.stopLoading();
            finishWithResult(ThreeDSecureAuthenticationResponse.fromException(description));
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            view.stopLoading();
            finishWithResult(ThreeDSecureAuthenticationResponse.fromException(error.toString()));
        }
    };

    private WebChromeClient mThreeDSecureWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress < 100) {
                setProgress(newProgress);
                setProgressBarVisibility(true);
            } else {
                setProgressBarVisibility(false);
            }
        }
    };

    private void finishWithResult(ThreeDSecureAuthenticationResponse threeDSecureAuthenticationResponse) {
        setResult(Activity.RESULT_OK,  new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        threeDSecureAuthenticationResponse));
        finish();
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            mActionBar = getActionBar();
            if (mActionBar != null) {
                setActionBarTitle("");
                mActionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void setActionBarTitle(String title) {
        if (mActionBar != null) {
            mActionBar.setTitle(title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        String originalUrl = mThreeDSecureWebView.copyBackForwardList().getCurrentItem().getOriginalUrl();
        if (!originalUrl.equals(mThreeDSecureLookup.getAcsUrl()) && mThreeDSecureWebView.canGoBack()) {
            mThreeDSecureWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
