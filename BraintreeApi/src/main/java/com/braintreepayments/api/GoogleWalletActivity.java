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
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.gson.Gson;

public class GoogleWalletActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    public static final int REQUEST_FAILED = 15;
    public static final int CONNECTION_FAILED = 16;
    public static final String EXTRA_CONNECTION_FAILED_ERROR_CODE = "com.braintreepayments.api.GoogleWalletActivity.EXTRA_CONNECTION_FAILED_ERROR_CODE";

    private static final int MASKED_WALLET_REQUEST = 100;
    private static final int FULL_WALLET_REQUEST = 200;

    private GoogleApiClient mGoogleApiClient;
    private GoogleWallet mGoogleWallet;
    private Cart mCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ClientToken clientToken =
                new Gson().fromJson(getIntent().getStringExtra("clientToken"), ClientToken.class);
        Configuration configuration =
                new Gson().fromJson(getIntent().getStringExtra("configuration"), Configuration.class);
        int environment = WalletConstants.ENVIRONMENT_SANDBOX;
        if(configuration.getAndroidPay() != null) {
            switch(configuration.getAndroidPay().getEnvironment()) {
                case "sandbox":
                    environment = WalletConstants.ENVIRONMENT_SANDBOX;
                    break;
                case "production":
                    environment = WalletConstants.ENVIRONMENT_PRODUCTION;
                    break;
                default:
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

        mCart = getIntent().getParcelableExtra("cart");
        mGoogleWallet = new GoogleWallet(clientToken, configuration, mCart);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wallet.Payments.loadMaskedWallet(mGoogleApiClient, mGoogleWallet.getMaskedWalletRequest(), MASKED_WALLET_REQUEST);
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
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MASKED_WALLET_REQUEST) {
                // no need to do full wallet here?
                // just do masked and return txn id

                MaskedWallet maskedWallet =
                        data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                FullWalletRequest fullWalletRequest = mGoogleWallet.getFullWalletRequest(
                        maskedWallet.getGoogleTransactionId());

                Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,
                        FULL_WALLET_REQUEST);
            } else if (requestCode == FULL_WALLET_REQUEST) {
                setResult(Activity.RESULT_OK, data);
                finish();
            }
        } else {
            setResult(REQUEST_FAILED, data);
            finish();
        }
    }
}
