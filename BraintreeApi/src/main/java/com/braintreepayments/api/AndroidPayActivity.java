package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.gson.Gson;

public class AndroidPayActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    public static final int REQUEST_FAILED = 15;
    public static final int CONNECTION_FAILED = 16;
    public static final String EXTRA_CONNECTION_FAILED_ERROR_CODE = "com.braintreepayments.api.AndroidPayActivity.EXTRA_CONNECTION_FAILED_ERROR_CODE";
    public static final String EXTRA_CLIENT_TOKEN = "com.braintreepayments.api.AndroidPayActivity.EXTRA_CLIENT_TOKEN";
    public static final String EXTRA_CONFIGURATION = "com.braintreepayments.api.AndroidPayActivity.EXTRA_CONFIGURATION";
    public static final String EXTRA_CART = "com.braintreepayments.api.AndroidPayActivity.EXTRA_CART";
    public static final String EXTRA_IS_BILLING_AGREEMENT = "com.braintreepayments.api.AndroidPayActivity.EXTRA_IS_BILLING_AGREEMENT";
    public static final String EXTRA_SHIPPING_ADDRESS_REQUIRED = "com.braintreepayments.api.AndroidPayActivity.EXTRA_SHIPPING_ADDRESS_REQUIRED";
    public static final String EXTRA_PHONE_NUMBER_REQUIRED = "com.braintreepayments.api.AndroidPayActivity.EXTRA_PHONE_NUMBER_REQUIRED";

    private static final int MASKED_WALLET_REQUEST = 100;
    private static final int FULL_WALLET_REQUEST = 200;

    private GoogleApiClient mGoogleApiClient;
    private AndroidPay mAndroidPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ClientToken clientToken =
                new Gson().fromJson(getIntent().getStringExtra(EXTRA_CLIENT_TOKEN), ClientToken.class);
        Configuration configuration =
                new Gson().fromJson(getIntent().getStringExtra(EXTRA_CONFIGURATION), Configuration.class);

        int environment = WalletConstants.ENVIRONMENT_SANDBOX;
        if(configuration.getAndroidPay().getEnvironment() != null) {
            if (configuration.getAndroidPay().getEnvironment().equals("production")) {
                environment = WalletConstants.ENVIRONMENT_PRODUCTION;
            } else {
                environment = WalletConstants.ENVIRONMENT_SANDBOX;
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(environment)
                        .setTheme(WalletConstants.THEME_HOLO_LIGHT)
                        .build())
                .build();
        mGoogleApiClient.connect();

        Cart cart = getIntent().getParcelableExtra(EXTRA_CART);
        mAndroidPay = new AndroidPay(configuration, cart);
    }

    @Override
    public void onConnected(Bundle bundle) {
        MaskedWalletRequest maskedWalletRequest = mAndroidPay.getMaskedWalletRequest(
                getBooleanExtra(EXTRA_IS_BILLING_AGREEMENT),
                getBooleanExtra(EXTRA_SHIPPING_ADDRESS_REQUIRED),
                getBooleanExtra(EXTRA_PHONE_NUMBER_REQUIRED));
        Wallet.Payments.loadMaskedWallet(mGoogleApiClient, maskedWalletRequest, MASKED_WALLET_REQUEST);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Intent intent = new Intent()
                .putExtra(EXTRA_CONNECTION_FAILED_ERROR_CODE, i);
        setResult(CONNECTION_FAILED, intent);
        finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Intent intent = new Intent()
                .putExtra(EXTRA_CONNECTION_FAILED_ERROR_CODE, connectionResult.getErrorCode());
        setResult(CONNECTION_FAILED, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == MASKED_WALLET_REQUEST) {
                // no need to do full wallet here?
                // just do masked and return txn id

                MaskedWallet maskedWallet =
                        data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                FullWalletRequest fullWalletRequest = mAndroidPay.getFullWalletRequest(
                        maskedWallet.getGoogleTransactionId());

                Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,
                        FULL_WALLET_REQUEST);
            } else if (requestCode == FULL_WALLET_REQUEST) {
                setResult(Activity.RESULT_OK, data);
                finish();
            }
        } else if (resultCode == RESULT_CANCELED) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            setResult(REQUEST_FAILED, data);
            finish();
        }
    }

    private boolean getBooleanExtra(String key) {
        return getIntent().getBooleanExtra(key, false);
    }
}
