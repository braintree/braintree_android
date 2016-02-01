package com.braintreepayments.api.threedsecure;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.widget.FrameLayout;

import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.models.ThreeDSecureLookup;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Stack;

public class ThreeDSecureWebViewActivity extends Activity {

    public static final String EXTRA_THREE_D_SECURE_LOOKUP = "com.braintreepayments.api.EXTRA_THREE_D_SECURE_LOOKUP";
    public static final String EXTRA_THREE_D_SECURE_RESULT = "com.braintreepayments.api.EXTRA_THREE_D_SECURE_RESULT";

    private ActionBar mActionBar;
    private FrameLayout mRootView;
    private Stack<ThreeDSecureWebView> mThreeDSecureWebViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        ThreeDSecureLookup threeDSecureLookup =
                getIntent().getParcelableExtra(EXTRA_THREE_D_SECURE_LOOKUP);
        if (threeDSecureLookup == null) {
            throw new IllegalArgumentException("A ThreeDSecureLookup must be specified with " +
                    ThreeDSecureLookup.class.getSimpleName() + ".EXTRA_THREE_D_SECURE_LOOKUP extra");
        }

        setupActionBar();

        mThreeDSecureWebViews = new Stack<ThreeDSecureWebView>();
        mRootView = ((FrameLayout) findViewById(android.R.id.content));

        StringBuilder params = new StringBuilder();
        try {
            params.append("PaReq=");
            params.append(URLEncoder.encode(threeDSecureLookup.getPareq(), "UTF-8"));
            params.append("&MD=");
            params.append(URLEncoder.encode(threeDSecureLookup.getMd(), "UTF-8"));
            params.append("&TermUrl=");
            params.append(URLEncoder.encode(threeDSecureLookup.getTermUrl(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            finish();
        }

        ThreeDSecureWebView webView = new ThreeDSecureWebView(this);
        webView.init(this);
        webView.postUrl(threeDSecureLookup.getAcsUrl(), params.toString().getBytes());
        pushNewWebView(webView);
    }

    protected void pushNewWebView(ThreeDSecureWebView webView) {
        mThreeDSecureWebViews.push(webView);
        mRootView.removeAllViews();
        mRootView.addView(webView);
    }

    protected void popCurrentWebView() {
        mThreeDSecureWebViews.pop();
        pushNewWebView(mThreeDSecureWebViews.pop());
    }

    protected void finishWithResult(ThreeDSecureAuthenticationResponse threeDSecureAuthenticationResponse) {
        setResult(Activity.RESULT_OK,  new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        threeDSecureAuthenticationResponse));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mThreeDSecureWebViews.peek().canGoBack()) {
            mThreeDSecureWebViews.peek().goBack();
        } else if (mThreeDSecureWebViews.size() > 1) {
            popCurrentWebView();
        } else {
            super.onBackPressed();
        }
    }

    protected void setActionBarTitle(String title) {
        if (mActionBar != null) {
            mActionBar.setTitle(title);
        }
    }

    private void setupActionBar() {
        mActionBar = getActionBar();
        if (mActionBar != null) {
            setActionBarTitle("");
            mActionBar.setDisplayHomeAsUpEnabled(true);
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
}
