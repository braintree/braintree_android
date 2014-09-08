package com.braintreepayments.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.braintreepayments.api.SignatureVerification;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int CUSTOM_REQUEST = 100;
    private static final int DROP_IN_REQUEST = 200;

    private AsyncHttpClient mHttpClient;
    private String mClientToken;
    private Button mBuyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBuyButton = (Button) findViewById(R.id.buy);

        mHttpClient = new AsyncHttpClient();

        // Warning, signature verification is disabled for this demo only, you should never
        // do this as it opens a security hole
        SignatureVerification.disableAppSwitchSignatureVerification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBuyButton.setEnabled(false);
        getClientToken();
    }

    private void getClientToken() {
        mHttpClient.get(OptionsActivity.getClientTokenUrl(this), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    mClientToken = new JSONObject(response).getString("client_token");
                    mBuyButton.setEnabled(true);
                } catch (JSONException e) {
                    showDialog("Unable to decode client token");
                }
            }

            @Override
            public void onFailure(int statusCode, Throwable error, String errorMessage) {
                showDialog("Unable to get a client token. Status code: " + statusCode + ". Error:" +
                        errorMessage);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.options) {
            startActivity(new Intent(this, OptionsActivity.class));
            return true;
        }
        return false;
    }

    public void onBuyClick(View v) {
        Intent intent = new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken);
        if (OptionsActivity.getFormType(this) == OptionsActivity.CUSTOM) {
            intent.setClass(this, CustomFormActivity.class);
            startActivityForResult(intent, CUSTOM_REQUEST);
        } else if (OptionsActivity.getFormType(this) == OptionsActivity.DROP_IN) {
            Customization customization = new CustomizationBuilder()
                    .primaryDescription("Cart")
                    .secondaryDescription("3 Items")
                    .amount("$1")
                    .submitButtonText("Buy")
                    .build();

            intent.setClass(this, BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
            startActivityForResult(intent, DROP_IN_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, FinishedActivity.class)
                    .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE,
                              data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE));
            startActivity(intent);
        }
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
            .setMessage(message)
            .show();
    }

}
