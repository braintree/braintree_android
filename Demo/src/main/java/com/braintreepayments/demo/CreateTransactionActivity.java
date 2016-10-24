package com.braintreepayments.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.demo.models.Transaction;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CreateTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_PAYMENT_METHOD_NONCE = "nonce";

    private ProgressBar mLoadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_transaction_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mLoadingSpinner = (ProgressBar) findViewById(R.id.loading_spinner);
        setTitle(R.string.processing_transaction);

        sendNonceToServer((PaymentMethodNonce) getIntent().getParcelableExtra(EXTRA_PAYMENT_METHOD_NONCE));
    }

    private void sendNonceToServer(PaymentMethodNonce nonce) {
        Callback<Transaction> callback = new Callback<Transaction>() {
            @Override
            public void success(Transaction transaction, Response response) {
                if (transaction.getMessage() != null &&
                        transaction.getMessage().startsWith("created")) {
                    setStatus(R.string.transaction_complete);
                    setMessage(transaction.getMessage());
                } else {
                    setStatus(R.string.transaction_failed);
                    if (TextUtils.isEmpty(transaction.getMessage())) {
                        setMessage("Server response was empty or malformed");
                    } else {
                        setMessage(transaction.getMessage());
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                setStatus(R.string.transaction_failed);
                setMessage("Unable to create a transaction. Response Code: " +
                        error.getResponse().getStatus() + " Response body: " +
                        error.getResponse().getBody());
            }
        };

        if (Settings.isThreeDSecureEnabled(this) && Settings.isThreeDSecureRequired(this)) {
            DemoApplication.getApiClient(this).createTransaction(nonce.getNonce(),
                    Settings.getThreeDSecureMerchantAccountId(this), true, callback);
        } else if (Settings.isThreeDSecureEnabled(this)) {
            DemoApplication.getApiClient(this).createTransaction(nonce.getNonce(),
                    Settings.getThreeDSecureMerchantAccountId(this), callback);
        } else if (nonce instanceof CardNonce && ((CardNonce) nonce).getCardType().equals("UnionPay")) {
            DemoApplication.getApiClient(this).createTransaction(nonce.getNonce(),
                    Settings.getUnionPayMerchantAccountId(this), callback);
        } else {
            DemoApplication.getApiClient(this).createTransaction(nonce.getNonce(), Settings.getMerchantAccountId(this),
                    callback);
        }
    }

    private void setStatus(int message) {
        mLoadingSpinner.setVisibility(View.GONE);
        setTitle(message);
        TextView status = (TextView) findViewById(R.id.transaction_status);
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }

    private void setMessage(String message) {
        mLoadingSpinner.setVisibility(View.GONE);
        TextView textView = (TextView) findViewById(R.id.transaction_message);
        textView.setText(message);
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
