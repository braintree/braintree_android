package com.braintreepayments.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.SecureLoadingProgressBar;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

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

        if (Settings.isThreeDSecureEnabled(this)) {
            if (Settings.getEnvironment(this) == 1) {
                params.put("merchant_account_id", "test_AIB");
            }

            params.put("requireThreeDSecure", Settings.isThreeDSecureRequired(this));
        }

        new AsyncHttpClient().post(Settings.getEnvironmentUrl(this) + "/nonce/transaction", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            showSuccessView(new JSONObject(response).optString("message"));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    private void showSuccessView(String message) {
        mLoadingSpinner.setVisibility(View.GONE);
        findViewById(R.id.thanks).setVisibility(View.VISIBLE);
        if (message != null) {
            TextView textView = (TextView) findViewById(R.id.transaction_id);
            textView.setText(message);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
