package com.braintreepayments.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.SecureLoadingProgressBar;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class FinishedActivity extends Activity {

    private SecureLoadingProgressBar mLoadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finished);
        mLoadingSpinner = (SecureLoadingProgressBar) findViewById(R.id.loading_spinner);

        sendNonceToServer(
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE));
    }

    private void sendNonceToServer(String nonce) {
        RequestParams params = new RequestParams();
        params.put("nonce", nonce);
        new AsyncHttpClient().post(OptionsActivity.getEnvironmentUrl(this) + "/nonce/transaction", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        showSuccessView();
                    }
                });
    }

    private void showSuccessView() {
        mLoadingSpinner.setVisibility(View.GONE);
        findViewById(R.id.thanks).setVisibility(View.VISIBLE);
    }

}
