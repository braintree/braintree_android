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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.BraintreePaymentActivity;
import com.braintreepayments.api.PayPalSignatureVerification;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodCreatedListener;
import com.braintreepayments.api.internal.VenmoSignatureVerification;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.ClientToken;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity implements PaymentMethodCreatedListener,
        BraintreeErrorListener, OnNavigationListener {

    private static final int DROP_IN_REQUEST = 100;
    private static final int PAYMENT_BUTTON_REQUEST = 200;
    private static final int CUSTOM_REQUEST = 300;
    private static final int PAYPAL_REQUEST = 400;

    /**
     * Keys to store state on config changes.
     */
    private static final String KEY_CLIENT_TOKEN = "clientToken";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_ENVIRONMENT = "environment";

    private String mClientToken;
    private BraintreeFragment mBraintreeFragment;
    private String mNonce;
    private int mEnvironment;

    private TextView mNonceTextView;
    private Button mDropInButton;
    private Button mPayPalButton;
    private Button mPaymentButtonButton;
    private Button mCustomButton;
    private Button mCreateTransactionButton;
    private ProgressDialog mLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Warning, signature verification is disabled for this demo only, you should never
        // do this as it opens a security hole
        PayPalSignatureVerification.disableAppSwitchSignatureVerification();
        VenmoSignatureVerification.disableAppSwitchSignatureVerification();

        mNonceTextView = (TextView) findViewById(R.id.nonce);
        mDropInButton = (Button) findViewById(R.id.drop_in);
        mPayPalButton = (Button) findViewById(R.id.paypal);
        mPaymentButtonButton = (Button) findViewById(R.id.payment_button);
        mCustomButton = (Button) findViewById(R.id.custom);
        mCreateTransactionButton = (Button) findViewById(R.id.create_transaction);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CLIENT_TOKEN)) {
                mClientToken = savedInstanceState.getString(KEY_CLIENT_TOKEN);
                setup();
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
        if (mClientToken == null && !Settings.useClientKey(this)) {
            getClientToken();
        } else if (Settings.useClientKey(this)) {
            setup();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mClientToken != null && Settings.useClientKey(this)) {
            resetState();
            setup();
        } else if (mClientToken == null && !Settings.useClientKey(this)) {
            resetState();
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
                .primaryDescription(getString(R.string.cart))
                .secondaryDescription("1 Item")
                .amount("$1.00")
                .submitButtonText(getString(R.string.buy))
                .build();

        Intent intent = new Intent(this, BraintreePaymentActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT,
                        Settings.isAndroidPayBillingAgreement(this));

        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_AUTHORIZATION, getAuthorization());

        startActivityForResult(intent, DROP_IN_REQUEST);
    }

    public void launchPayPal(View v) {
        Intent intent = PayPalActivity.createIntent(this);

        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_AUTHORIZATION, getAuthorization());

        startActivityForResult(intent, PAYPAL_REQUEST);
    }

    public void launchPaymentButton(View v) {
        Intent intent = new Intent(this, PaymentButtonActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT,
                        Settings.isAndroidPayBillingAgreement(this))
                .putExtra("shippingAddressRequired",
                        Settings.isAndroidPayShippingAddressRequired(this))
                .putExtra("phoneNumberRequired", Settings.isAndroidPayPhoneNumberRequired(this))
                .putExtra("payPalAddressScopeRequested", Settings.isPayPalAddressScopeRequested(
                        this));

        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_AUTHORIZATION, getAuthorization());

        startActivityForResult(intent, PAYMENT_BUTTON_REQUEST);
    }

    public void launchCustom(View v) {
        Intent intent = new Intent(this, CustomFormActivity.class)
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_CART, getAndroidPayCart())
                .putExtra(BraintreePaymentActivity.EXTRA_ANDROID_PAY_IS_BILLING_AGREEMENT,
                        Settings.isAndroidPayBillingAgreement(this))
                .putExtra("shippingAddressRequired",
                        Settings.isAndroidPayShippingAddressRequired(this))
                .putExtra("phoneNumberRequired", Settings.isAndroidPayPhoneNumberRequired(this))
                .putExtra("payPalAddressScopeRequested",
                        Settings.isPayPalAddressScopeRequested(this));

        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_AUTHORIZATION, getAuthorization());

        startActivityForResult(intent, CUSTOM_REQUEST);
    }

    public void createTransaction(View v) {
        Intent intent = new Intent(this, FinishedActivity.class)
                .putExtra(FinishedActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);

        mCreateTransactionButton.setEnabled(false);
        mNonceTextView.setText("");
        mNonce = null;
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
        super.onActivityResult(requestCode, resultCode, data);

        safelyCloseLoadingView();

        if (resultCode == RESULT_OK) {
            PaymentMethod paymentMethod =
                    data.getParcelableExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);
            displayNonce(paymentMethod.getNonce());
            if (Settings.isThreeDSecureEnabled(this)) {
                mLoading = ProgressDialog.show(this, getString(R.string.loading),
                        getString(R.string.loading), true, false);
                ThreeDSecure.performVerification(mBraintreeFragment, mNonce, "1");
            } else {
                mCreateTransactionButton.setEnabled(true);
            }
        } else if (resultCode != RESULT_CANCELED) {
            safelyCloseLoadingView();
            showDialog(data.getStringExtra(BraintreePaymentActivity.EXTRA_ERROR_MESSAGE));
        }
    }

    private void getClientToken() {
        new RestAdapter.Builder()
                .setEndpoint(Settings.getEnvironmentUrl(this))
                .setRequestInterceptor(new ApiClientRequestInterceptor())
                .build()
                .create(ApiClient.class)
                .getClientToken(Settings.getCustomerId(this),
                        Settings.getThreeDSecureMerchantAccountId(this),
                        new Callback<ClientToken>() {
                            @Override
                            public void success(ClientToken clientToken, Response response) {
                                if (TextUtils.isEmpty(clientToken.getClientToken())) {
                                    showDialog("Client token was empty");
                                } else {
                                    mClientToken = clientToken.getClientToken();
                                    setup();
                                }
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                showDialog("Unable to get a client token. Response Code: " +
                                        error.getResponse().getStatus() + " Response body: " +
                                        error.getResponse().getBody());
                            }
                        });
    }

    private void resetState() {
        enableButtons(false);
        mCreateTransactionButton.setEnabled(false);
        mNonceTextView.setText("");

        if (mBraintreeFragment != null) {
            getFragmentManager().beginTransaction().remove(mBraintreeFragment).commit();
        }

        mClientToken = null;
        mBraintreeFragment = null;
        mNonce = null;

        if (!Settings.useClientKey(this)) {
            getClientToken();
        } else {
            setup();
        }
    }

    private void setup() {
        try {
            String authorization;
            if (Settings.useClientKey(this)) {
                authorization = Settings.getEnvironmentClientKey(this);
            } else {
                authorization = mClientToken;
            }
            mBraintreeFragment = BraintreeFragment.newInstance(this, authorization);
            enableButtons(true);
        } catch (InvalidArgumentException e) {
            showDialog(e.getMessage());
        }
    }

    private String getAuthorization() {
        if (Settings.useClientKey(this)) {
            return Settings.getEnvironmentClientKey(this);
        } else {
            return mClientToken;
        }
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
        mPayPalButton.setEnabled(enable);
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
