package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.MenuItem;
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

    private WebView mThreeDSecureWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThreeDSecureLookup threeDSecureLookup =
                getIntent().getParcelableExtra(EXTRA_THREE_D_SECURE_LOOKUP);
        if (threeDSecureLookup == null) {
            throw new IllegalArgumentException("A ThreeDSecureLookup must be specified with " +
                    ThreeDSecureLookup.class.getSimpleName() + ".EXTRA_THREE_D_SECURE_LOOKUP extra");
        }

        setupActionBar();

        mThreeDSecureWebView = new WebView(this);
        mThreeDSecureWebView.setId(android.R.id.widget_frame);
        mThreeDSecureWebView.setWebViewClient(threeDSecureWebViewClient());
        ((FrameLayout) findViewById(android.R.id.content)).addView(mThreeDSecureWebView);

        mThreeDSecureWebView.getSettings().setJavaScriptEnabled(true);
        mThreeDSecureWebView.getSettings().setBuiltInZoomControls(true);
        mThreeDSecureWebView.getSettings().setUserAgentString(HttpRequest.USER_AGENT);
        mThreeDSecureWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("PaReq", threeDSecureLookup.getPareq()));
        params.add(new BasicNameValuePair("MD", threeDSecureLookup.getMd()));
        params.add(new BasicNameValuePair("TermUrl", threeDSecureLookup.getTermUrl()));
        ByteArrayOutputStream encodedParams = new ByteArrayOutputStream();
        try {
            new UrlEncodedFormEntity(params, HTTP.UTF_8).writeTo(encodedParams);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mThreeDSecureWebView.postUrl(threeDSecureLookup.getAcsUrl(), encodedParams.toByteArray());
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle("");
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private WebViewClient threeDSecureWebViewClient() {
        return new WebViewClient() {
            public void onPageStarted(WebView view, String url, Bitmap icon) {
                if (url.contains("html/authentication_complete_frame")) {
                    view.stopLoading();

                    String authResponseJson = (Uri.parse(url).getQueryParameter("auth_response"));

                    ThreeDSecureAuthenticationResponse authResponse =
                            ThreeDSecureAuthenticationResponse.fromJson(authResponseJson);
                    setResult(Activity.RESULT_OK, new Intent()
                            .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                                    authResponse));
                    finish();
                } else {
                    super.onPageStarted(view, url, icon);
                }
            }
        };
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
        if (mThreeDSecureWebView.canGoBack()) {
            mThreeDSecureWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
