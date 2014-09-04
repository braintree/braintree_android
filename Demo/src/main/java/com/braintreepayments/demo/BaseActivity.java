package com.braintreepayments.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseActivity extends Activity implements PaymentMethodNonceListener {

    protected static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    protected static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";

    protected AsyncHttpClient mHttpClient;
    protected String mEnvironmentSwitch;
    protected Braintree mBraintree;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHttpClient = new AsyncHttpClient();
        mEnvironmentSwitch = getIntent().getStringExtra(MainActivity.EXTRA_ENVIRONMENT);
        getClientToken();
    }

    public abstract void ready(String clientToken);

    @Override
    public void onPaymentMethodNonce(String nonce) {
        RequestParams params = new RequestParams();
        params.put("nonce", nonce);
        mHttpClient.post(getBaseUrl() + "/nonce/transaction", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        showDialog(response);
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private void getClientToken() {
        mHttpClient.get(getBaseUrl() + "/client_token", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    ready(json.getString("client_token"));
                } catch (JSONException e) {
                    showDialog("Unable to fetch a client token!");
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable error, String errorMessage) {
                showDialog("Unable to get a client token. Status code: " + statusCode + ". Error:" + errorMessage);
            }
        });
    }

    protected String getBaseUrl() {
        if(mEnvironmentSwitch.equals("production")) {
            return PRODUCTION_BASE_SERVER_URL;
        } else if (mEnvironmentSwitch.equals("sandbox")) {
            return SANDBOX_BASE_SERVER_URL;
        }

        return "";
    }

    protected void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .show();
    }
}
