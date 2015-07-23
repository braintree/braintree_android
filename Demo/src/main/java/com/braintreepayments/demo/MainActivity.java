package com.braintreepayments.demo;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.SignatureVerification;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.demo.internal.BraintreeHttpRequest;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.braintreepayments.api.PayPalCheckout;

import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;

import java.io.IOException;

public class MainActivity extends Activity implements PaymentMethodCreatedListener, ErrorListener,
    OnNavigationListener {

    private static final int DROP_IN_REQUEST = 100;
    private static final int PAYMENT_BUTTON_REQUEST = 200;
    private static final int CUSTOM_REQUEST = 300;
    private static final int THREE_D_SECURE_REQUEST = 400;
    private static final int PAYPAL_CHECKOUT_REQUEST = 500;


    /**
     * Keys to store state on config changes.
     */
    private static final String KEY_CLIENT_TOKEN = "clientToken";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_ENVIRONMENT = "environment";

    private OkHttpClient mHttpClient;

    private String mClientToken;
    private Braintree mBraintree;
    private String mNonce;
    private int mEnvironment;

    private TextView mNonceTextView;
    private Button mDropInButton;
    private Button mPaymentButtonButton;
    private Button mCustomButton;
    private Button mCheckoutButton;
    private Button mCreateTransactionButton;
    private ProgressDialog mLoading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHttpClient = new OkHttpClient();
        mHttpClient.networkInterceptors().add(new BraintreeHttpRequest());

        // Warning, signature verification is disabled for this demo only, you should never
        // do this as it opens a security hole
        SignatureVerification.disableAppSwitchSignatureVerification();

        mNonceTextView = (TextView) findViewById(R.id.nonce);
        mDropInButton = (Button) findViewById(R.id.drop_in);
        mPaymentButtonButton = (Button) findViewById(R.id.payment_button);
        mCustomButton = (Button) findViewById(R.id.custom);
        mCheckoutButton = (Button) findViewById(R.id.checkout);
        mCreateTransactionButton = (Button) findViewById(R.id.create_transaction);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CLIENT_TOKEN)) {
                setClientToken(savedInstanceState.getString(KEY_CLIENT_TOKEN));
            }
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                mNonce = savedInstanceState.getString(KEY_NONCE);
            }
            mEnvironment = savedInstanceState.getInt(KEY_ENVIRONMENT);
        } else {
            mEnvironment = PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt(Settings.ENVIRONMENT, 0);
        }
        setupActionBar();
        if (mClientToken == null) {
            getClientToken();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mClientToken != null) {
            outState.putString(KEY_CLIENT_TOKEN, mClientToken);
        }
        if (mNonce != null) {
            outState.putString(KEY_NONCE, mNonce);
        }
        outState.putInt(KEY_ENVIRONMENT, mEnvironment);
    }

    public void launchDropIn(View v) {
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Cart")
                .secondaryDescription("1 Item")
                .amount("$1.00")
                .submitButtonText("Buy")
                .build();

        Intent intent = new Intent(this, BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken)
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT,
                        Settings.isAndroidPayBillingAgreement(this));

        startActivityForResult(intent, DROP_IN_REQUEST);
    }

    public void launchPaymentButton(View v) {
        Intent intent = new Intent(this, PaymentButtonActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT, Settings.isAndroidPayBillingAgreement(this))
                .putExtra("shippingAddressRequired", Settings.isAndroidPayShippingAddressRequired(this))
                .putExtra("phoneNumberRequired", Settings.isAndroidPayPhoneNumberRequired(this))
                .putExtra("payPalAddressScopeRequested", Settings.isPayPalAddressScopeRequested(this));

        startActivityForResult(intent, PAYMENT_BUTTON_REQUEST);
    }

    public void launchCustom(View v) {
        Intent intent = new Intent(this, CustomFormActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT, Settings.isAndroidPayBillingAgreement(this))
                .putExtra("shippingAddressRequired", Settings.isAndroidPayShippingAddressRequired(this))
                .putExtra("phoneNumberRequired", Settings.isAndroidPayPhoneNumberRequired(this))
                .putExtra("payPalAddressScopeRequested", Settings.isPayPalAddressScopeRequested(this));

        startActivityForResult(intent, CUSTOM_REQUEST);
    }

    public void launchCheckout(View v) {
        PayPalCheckout checkout = new PayPalCheckout(BigDecimal.ONE);
        mBraintree.startCheckoutWithPayPal(MainActivity.this, PAYPAL_CHECKOUT_REQUEST, checkout);
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
        showDialog("An unrecoverable error was encountered (" +
                throwable.getClass().getSimpleName() + "): " + throwable.getMessage());
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        safelyCloseLoadingView();
        showDialog("A recoverable error occurred: " + error.getMessage());
    }

    @Override
    public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
        displayNonce(paymentMethod.getNonce());
        safelyCloseLoadingView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // required due to the conflicting Braintree instances in this unusual integration
        if (mBraintree != null) {
            mBraintree.unlockListeners();
        }

        safelyCloseLoadingView();

        if (resultCode == RESULT_OK) {
            if (requestCode == THREE_D_SECURE_REQUEST) {
                mBraintree.finishThreeDSecureVerification(resultCode, data);
            } else {
                if (data.hasExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD)) {
                    PaymentMethod paymentMethod = data.getParcelableExtra(
                            BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);
                    displayNonce(paymentMethod.getNonce());
                } else {
                    displayNonce(data.getStringExtra(
                            BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE));
                }


                if (Settings.isThreeDSecureEnabled(this) && mBraintree.isThreeDSecureEnabled()) {
                    mLoading = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading), true, false);
                    mBraintree.startThreeDSecureVerification(this, THREE_D_SECURE_REQUEST, mNonce, "1");
                } else {
                    mCreateTransactionButton.setEnabled(true);
                }
            }
        } else if (resultCode != RESULT_CANCELED) {
            safelyCloseLoadingView();
            showDialog(data.getStringExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
        }
    }

    private void getClientToken() {
        Request getClientTokenRequest = new Request.Builder()
                .url(Settings.getClientTokenUrl(this))
                .build();

        mHttpClient.newCall(getClientTokenRequest).enqueue(new Callback() {
            @Override
            public void onResponse(final Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            try {
                                setClientToken(
                                        new JSONObject(responseBody).getString("client_token"));
                            } catch (JSONException e) {
                                showDialog("Unable to decode client token");
                            }
                        } else {
                            showDialog("Unable to get a client token. Response Code: " +
                                    response.code() + " Response body: " + responseBody);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Request request, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("Unable to get a client token. Error:" + e.getMessage());
                    }
                });
            }
        });
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

    private void setClientToken(String clientToken) {
        mClientToken = clientToken;
        Braintree.setup(MainActivity.this, mClientToken, new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful,
                    Braintree braintree,
                    String errorMessage, Exception exception) {
                if (setupSuccessful) {
                    mBraintree = braintree;
                    mBraintree.addListener(MainActivity.this);
                    enableButtons(true);
                } else {
                    showDialog(errorMessage);
                }
            }
        });

        enableButtons(true);
    }

    private void displayNonce(String nonce) {
        mNonce = nonce;
        mNonceTextView.setText(getString(R.string.nonce) + ": " + nonce);
        mCreateTransactionButton.setEnabled(true);
    }

    private Cart getAndroidPayCart() {
        if (Settings.isAndroidPayBillingAgreement(this)) {
            return null;
        } else {
            return Cart.newBuilder()
                    .setCurrencyCode("USD")
                    .setTotalPrice("1.00")
                    .addLineItem(LineItem.newBuilder()
                            .setCurrencyCode("USD")
                            .setDescription("Description")
                            .setQuantity("1")
                            .setUnitPrice("1.00")
                            .setTotalPrice("1.00")
                            .build())
                    .build();
        }
    }

    private void enableButtons(boolean enable) {
        mDropInButton.setEnabled(enable);
        mPaymentButtonButton.setEnabled(enable);
        mCustomButton.setEnabled(enable);
        mCheckoutButton.setEnabled(enable);
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

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.environments, android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, this);
        actionBar.setSelectedNavigationItem(mEnvironment);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (mEnvironment != itemPosition) {
            mEnvironment = itemPosition;
            Settings.setEnvironment(this, itemPosition);
            resetState();
        }
        return true;
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
            return true;
        } else if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }
}
