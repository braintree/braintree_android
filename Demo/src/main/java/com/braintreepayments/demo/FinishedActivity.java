package com.braintreepayments.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.SecureLoadingProgressBar;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

@SuppressWarnings("com.braintreepayments.beta")
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
        ApiClient apiClient = new RestAdapter.Builder()
                .setEndpoint(Settings.getEnvironmentUrl(this))
                .setRequestInterceptor(new ApiClientRequestInterceptor())
                .build()
                .create(ApiClient.class);

        String merchantAccountId = null;
        boolean requireThreeDSecure = false;
        if (Settings.isThreeDSecureEnabled(this)) {
            merchantAccountId = Settings.getThreeDSecureMerchantAccountId(this);
            requireThreeDSecure = Settings.isThreeDSecureRequired(this);
        }

        apiClient.createTransaction(nonce, merchantAccountId, requireThreeDSecure,
                new Callback<Transaction>() {
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
                });
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
