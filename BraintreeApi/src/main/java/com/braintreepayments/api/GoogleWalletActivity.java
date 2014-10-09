package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

public class GoogleWalletActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final int MASKED_WALLET_REQUEST = 1817;
    private static final int FULL_WALLET_REQUEST = 1818;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_SANDBOX)
                        .setTheme(WalletConstants.THEME_HOLO_LIGHT)
                        .build())
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        MaskedWalletRequest maskedWalletRequest =
                MaskedWalletRequest.newBuilder()
                        .setMerchantName("Braintree Demo")
                        .setCurrencyCode("USD")
                        .setEstimatedTotalPrice("150.00")
                        .build();

        Wallet.Payments.loadMaskedWallet(mGoogleApiClient, maskedWalletRequest, MASKED_WALLET_REQUEST);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MASKED_WALLET_REQUEST) {
                MaskedWallet maskedWallet =
                        data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                        .setCart(Cart.newBuilder()
                                .setCurrencyCode("USD")
                                .setTotalPrice("150.00")
                                .addLineItem(LineItem.newBuilder()
                                        .setCurrencyCode("150.00")
                                        .setDescription("Description")
                                        .setQuantity("1")
                                        .setUnitPrice("150.00")
                                        .setTotalPrice("150.00")
                                        .build())
                                .build())
                        .setGoogleTransactionId(maskedWallet.getGoogleTransactionId())
                        .build();

                Wallet.Payments
                        .loadFullWallet(mGoogleApiClient, fullWalletRequest, FULL_WALLET_REQUEST);
            } else if (requestCode == FULL_WALLET_REQUEST) {
                setResult(Activity.RESULT_OK, data);
                finish();
            }
        }
    }

}
