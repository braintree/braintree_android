package com.braintreepayments.demo;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.SignatureVerification;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements PaymentMethodNonceListener, ErrorListener,
        OnNavigationListener {

    private static final int DROP_IN_REQUEST = 100;
    private static final int PAYMENT_BUTTON_REQUEST = 200;
    private static final int CUSTOM_REQUEST = 300;
    private static final int THREE_D_SECURE_REQUEST = 400;

    private SharedPreferences mPrefs;
    private AsyncHttpClient mHttpClient;

    private String mClientToken;
    private Braintree mBraintree;

    private String mNonce;

    private TextView mNonceTextView;
    private Button mDropInButton;
    private Button mPaymentButtonButton;
    private Button mCustomButton;
    private Button mCreateTransactionButton;
    private ProgressDialog mLoading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHttpClient = new AsyncHttpClient();

        // Warning, signature verification is disabled for this demo only, you should never
        // do this as it opens a security hole
        SignatureVerification.disableAppSwitchSignatureVerification();

        mNonceTextView = (TextView) findViewById(R.id.nonce);
        mDropInButton = (Button) findViewById(R.id.drop_in);
        mPaymentButtonButton = (Button) findViewById(R.id.payment_button);
        mCustomButton = (Button) findViewById(R.id.custom);
        mCreateTransactionButton = (Button) findViewById(R.id.create_transaction);

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
        actionBar.setSelectedNavigationItem(mPrefs.getInt(Settings.ENVIRONMENT, 0));
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Settings.setEnvironment(this, itemPosition);
        resetState();
        return true;
    }

    private void resetState() {
        enableButtons(false);
        mCreateTransactionButton.setEnabled(false);
        mNonceTextView.setText("");

        mClientToken = null;
        mBraintree = null;
        mNonce = null;

        getClientToken();
    }

    @SuppressWarnings("deprecation")
    private void getClientToken() {
        mHttpClient.get(Settings.getClientTokenUrl(this), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    mClientToken = new JSONObject(response).getString("client_token");
                    mBraintree = Braintree.getInstance(MainActivity.this, mClientToken);
                    mBraintree.addListener(MainActivity.this);

                    enableButtons(true);
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

    public void launchDropIn(View v) {
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Cart")
                .secondaryDescription("3 Items")
                .amount("$1")
                .submitButtonText("Buy")
                .build();

        Intent intent = new Intent(this, BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken)
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);

        startActivityForResult(intent, DROP_IN_REQUEST);
    }

    public void launchPaymentButton(View v) {
        Intent intent = new Intent(this, PaymentButtonActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken);

        startActivityForResult(intent, PAYMENT_BUTTON_REQUEST);
    }

    public void launchCustom(View v) {
        Intent intent = new Intent(this, CustomFormActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken);

        startActivityForResult(intent, CUSTOM_REQUEST);
    }

    public void createTransaction(View v) {
        Intent intent = new Intent(this, FinishedActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);
        resetState();
    }

    @Override
    public void onUnrecoverableError(Throwable throwable) {
        safelyCloseLoadingView();
        showDialog("An unrecoverable error was encountered (" + throwable.getClass().getSimpleName() +
                "): " + throwable.getMessage());
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        safelyCloseLoadingView();
        showDialog("A recoverable error occurred: " + error.getMessage());
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        mNonce = paymentMethodNonce;
        setNonce(mNonce);
        mCreateTransactionButton.setEnabled(true);

        safelyCloseLoadingView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // required due to the conflicting Braintree instances in this unusual integration
        mBraintree.unlockListeners();

        safelyCloseLoadingView();

        if (resultCode == RESULT_OK) {
            if (requestCode == THREE_D_SECURE_REQUEST) {
                mBraintree.finishThreeDSecureVerification(resultCode, data);
            } else {
                mNonce = data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);
                setNonce(mNonce);

                if (Settings.isThreeDSecureEnabled(this) && mBraintree.isThreeDSecureEnabled()) {
                    mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);
                    mBraintree.startThreeDSecureVerification(this, THREE_D_SECURE_REQUEST, mNonce, "1");
                } else {
                    mCreateTransactionButton.setEnabled(true);
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            mNonceTextView.setText("Canceled");
        }
    }

    private void setNonce(String nonce) {
        mNonceTextView.setText(getString(R.string.nonce) + ": " + nonce);
    }

    private void enableButtons(boolean enable) {
        mDropInButton.setEnabled(enable);
        mPaymentButtonButton.setEnabled(enable);
        mCustomButton.setEnabled(enable);
    }

    private void safelyCloseLoadingView() {
        if (mLoading != null && mLoading.isShowing()) {
            mLoading.dismiss();
        }
    }
    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
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
        } else if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }
}
