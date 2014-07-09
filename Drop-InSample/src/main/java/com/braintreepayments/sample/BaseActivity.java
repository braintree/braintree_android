package com.braintreepayments.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

import com.braintreepayments.api.Braintree;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public abstract class BaseActivity extends Activity {

    protected static final String BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";

    protected AsyncHttpClient mHttpClient;

    protected Braintree mBraintree;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHttpClient = new AsyncHttpClient();
        getClientToken();
    }

    public abstract void ready(String clientToken);

    protected void postNonceToServer(String nonce) {
        RequestParams params = new RequestParams();
        params.put("nonce", nonce);
        mHttpClient.post(BASE_SERVER_URL + "/nonce/transaction", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        showDialog(response);
                    }
                });
    }

    private void getClientToken() {
        mHttpClient.get(BASE_SERVER_URL + "/client_token", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                ready(response);
            }

            @Override
            public void onFailure(int statusCode, Throwable error, String errorMessage) {
                showDialog("Unable to get a client token. Status code: " + statusCode + ". Error:" + errorMessage);
            }
        });
    }

    protected void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .show();
    }
}
