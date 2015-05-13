package com.braintreepayments.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.SecureLoadingProgressBar;
import com.braintreepayments.demo.internal.BraintreeHttpRequest;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.networkInterceptors().add(new BraintreeHttpRequest());

        FormEncodingBuilder params = new FormEncodingBuilder();
        params.add("nonce", nonce);
        if (Settings.isThreeDSecureEnabled(this)) {
            if (Settings.getEnvironment(this) == 1) {
                params.add("merchant_account_id", "test_AIB");
            }

            params.add("requireThreeDSecure", String.valueOf(Settings.isThreeDSecureRequired(this)));
        }

        Request request = new Request.Builder()
                .url(Settings.getEnvironmentUrl(this) + "/nonce/transaction")
                .post(params.build())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            try {
                                showMessage(new JSONObject(response.body().string()).optString("message"));
                            } catch (JSONException e) {
                                showMessage(e.getMessage());
                            } catch (IOException e) {
                                showMessage(e.getMessage());
                            }
                        } else {
                            showMessage("Server responded with a non 200 response code.");
                        }
                    }
                });
            }

            @Override
            public void onFailure(Request request, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessage(e.getMessage());
                    }
                });
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
