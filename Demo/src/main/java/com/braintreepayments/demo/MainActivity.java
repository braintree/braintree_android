package com.braintreepayments.demo;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.SignatureVerification;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ThreeDSecureInfo;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements PaymentMethodNonceListener, ErrorListener,
        OnNavigationListener {

    private static final int CUSTOM_REQUEST = 100;
    private static final int DROP_IN_REQUEST = 200;
    private static final int THREE_D_SECURE_REQUEST = 300;

    private SharedPreferences mPrefs;
    private AsyncHttpClient mHttpClient;

    private String mClientToken;
    private Braintree mBraintree;

    private Button mPaymentInfoButton;
    private Button mThreeDSecureButton;
    private Button mPurchaseButton;

    private String mNonce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHttpClient = new AsyncHttpClient();

        // Warning, signature verification is disabled for this demo only, you should never
        // do this as it opens a security hole
        SignatureVerification.disableAppSwitchSignatureVerification();

        mPaymentInfoButton = (Button) findViewById(R.id.add_payment_info);
        mThreeDSecureButton = (Button) findViewById(R.id.perform_three_d_secure_verification);
        mPurchaseButton = (Button) findViewById(R.id.complete_purchase);

        setupActionBar();
        getClientToken();
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.environments, android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, this);
        actionBar.setSelectedNavigationItem(mPrefs.getInt(OptionsActivity.ENVIRONMENT, 0));
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        mPrefs.edit().putInt(OptionsActivity.ENVIRONMENT, itemPosition).apply();
        resetState();
        return true;
    }

    private void resetState() {
        mPaymentInfoButton.setEnabled(false);
        mThreeDSecureButton.setVisibility(View.GONE);
        mPurchaseButton.setVisibility(View.GONE);

        mClientToken = null;
        mBraintree = null;
        mNonce = null;

        getClientToken();
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    private void getClientToken() {
        mHttpClient.get(OptionsActivity.getClientTokenUrl(this), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    mClientToken = new JSONObject(response).getString("client_token");
                    mBraintree = Braintree.getInstance(MainActivity.this, mClientToken);
                    mBraintree.addListener(MainActivity.this);
                    mPaymentInfoButton.setEnabled(true);
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
        if (item.getItemId() == R.id.reset) {
            resetState();
        } else if (item.getItemId() == R.id.options) {
            startActivity(new Intent(this, OptionsActivity.class));
            return true;
        }
        return false;
    }

    public void onAddPaymentInformationClick(View v) {
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

    public void onPerformThreeDSecureVerificationClick(View v) {
        mBraintree.startThreeDSecureVerification(this, THREE_D_SECURE_REQUEST, mNonce, "1");
    }

    public void onCompletePurchaseClick(View v) {
        Intent intent = new Intent(this, FinishedActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);
    }

    @Override
    public void onUnrecoverableError(Throwable throwable) {
        showDialog("An unrecoverable error was encountered (" + throwable.getClass().getSimpleName() +
                "): " + throwable.getMessage());
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        if (error.getErrorInfo() instanceof ThreeDSecureInfo) {
            ThreeDSecureInfo threeDSecureInfo = (ThreeDSecureInfo) error.getErrorInfo();
            showDialog("3D Secure Error. liabilityShifted: " + threeDSecureInfo.isLiabilityShifted() +
                " liabilityShiftPossible: " + threeDSecureInfo.isLiabilityShiftPossible());
        } else {
            showDialog("A recoverable error occurred: " + error.getMessage());
        }
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        mNonce = paymentMethodNonce;
        mPurchaseButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // required due to the conflicting Braintree instances
        mBraintree.unlockListeners();

        if (resultCode == RESULT_OK) {
            if (requestCode == THREE_D_SECURE_REQUEST) {
                mBraintree.finishThreeDSecureVerification(resultCode, data);
                mThreeDSecureButton.setVisibility(View.GONE);
                mPurchaseButton.setVisibility(View.GONE);
            } else {
                mNonce = data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);

                if (mBraintree.isThreeDSecureEnabled()) {
                    mThreeDSecureButton.setVisibility(View.VISIBLE);
                } else {
                    mThreeDSecureButton.setVisibility(View.GONE);
                }

                mPurchaseButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
            .setMessage(message)
            .show();
    }
}
