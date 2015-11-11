package com.braintreepayments.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.dropin.view.SecureLoadingProgressBar;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FinishedActivity extends Activity {

    public static final String EXTRA_PAYMENT_METHOD_NONCE = "nonce";

    private SecureLoadingProgressBar mLoadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finished);
        mLoadingSpinner = (SecureLoadingProgressBar) findViewById(R.id.loading_spinner);

        sendNonceToServer(getIntent().getStringExtra(EXTRA_PAYMENT_METHOD_NONCE));
    }

    private void sendNonceToServer(String nonce) {
        Callback<Transaction> callback = new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                if (TextUtils.isEmpty(transaction.getMessage())) {
                    showMessage("Message was empty");
                } else {
                    showMessage(transaction.getMessage());
                }
            }

            @Override
            public void failure(RetrofitError error) {
                showMessage("Unable to create a transaction. Response Code: " +
                        error.getResponse().getStatus() + " Response body: " +
                        error.getResponse().getBody());
            }
        };

        if (Settings.isThreeDSecureEnabled(this) && Settings.isThreeDSecureRequired(this)) {
            DemoApplication.getApiClient(this).createTransaction(nonce,
                    Settings.getThreeDSecureMerchantAccountId(this), true, callback);
        } else if (Settings.isThreeDSecureEnabled(this)) {
            DemoApplication.getApiClient(this).createTransaction(nonce,
                    Settings.getThreeDSecureMerchantAccountId(this), callback);
        } else {
            DemoApplication.getApiClient(this).createTransaction(nonce, callback);
        }
    }

    private void showMessage(String message) {
        mLoadingSpinner.setVisibility(View.GONE);
        findViewById(R.id.thanks).setVisibility(View.VISIBLE);
        if (message != null) {
            TextView textView = (TextView) findViewById(R.id.transaction_id);
            textView.setText(message);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
