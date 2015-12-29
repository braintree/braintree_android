package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.braintreepayments.api.Braintree.BraintreeResponseListener;
import com.braintreepayments.api.annotations.Beta;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

/**
 * Class containing Android Pay specific logic.
 */
@Beta
public class AndroidPay {

    private Configuration mConfiguration;
    private GoogleApiClient mGoogleApiClient;
    private Cart mCart;

    protected AndroidPay(Configuration configuration) {
        mConfiguration = configuration;
    }

    protected void setCart(Cart cart) {
        mCart = cart;
    }

    /**
     * Checks the given {@link Intent} to see if it contains a {@link com.google.android.gms.wallet.MaskedWallet}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link WalletConstants#EXTRA_MASKED_WALLET},
     *         {@code false} otherwise.
     */
    @Beta
    public static boolean isMaskedWalletResponse(Intent intent) {
        return intent.hasExtra(WalletConstants.EXTRA_MASKED_WALLET);
    }

    /**
     * Checks the given {@link Intent} to see if it contains a {@link com.google.android.gms.wallet.FullWallet}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link WalletConstants#EXTRA_FULL_WALLET},
     *         {@code false} otherwise.
     */
    @Beta
    public static boolean isFullWalletResponse(Intent intent) {
        return intent.hasExtra(WalletConstants.EXTRA_FULL_WALLET);
    }

    protected void isReadyToPay(Context context, final BraintreeResponseListener<Boolean> listener)
            throws UnexpectedException {
        Wallet.Payments.isReadyToPay(getConnectedApiClient(context)).setResultCallback(
                new ResultCallback<BooleanResult>() {
                    @Override
                    public void onResult(@NonNull BooleanResult booleanResult) {
                        listener.onResponse(booleanResult.getStatus().isSuccess()
                                && booleanResult.getValue());
                    }
                });
    }

    protected void performMaskedWalletRequest(Context context, int requestCode,
            boolean isBillingAgreement, boolean shippingAddressRequired,
            boolean phoneNumberRequired) throws UnexpectedException {
        MaskedWalletRequest maskedWalletRequest = getMaskedWalletRequest(isBillingAgreement,
                shippingAddressRequired, phoneNumberRequired);
        Wallet.Payments.loadMaskedWallet(getConnectedApiClient(context), maskedWalletRequest,
                requestCode);
    }

    protected void performChangeMaskedWalletRequest(Context context, int requestCode,
            String googleTransactionId) throws UnexpectedException {
        Wallet.Payments.changeMaskedWallet(getConnectedApiClient(context), googleTransactionId,
                null, requestCode);
    }

    protected void performFullWalletRequest(Context context, int requestCode,
            String googleTransactionId) throws UnexpectedException {
        Wallet.Payments.loadFullWallet(getConnectedApiClient(context),
                getFullWalletRequest(googleTransactionId), requestCode);
    }

    protected PaymentMethodTokenizationParameters getTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.PAYMENT_GATEWAY)
                .addParameter("gateway", "braintree")
                .addParameter("braintree:merchantId", mConfiguration.getMerchantId())
                .addParameter("braintree:authorizationFingerprint",
                        mConfiguration.getAndroidPay().getGoogleAuthorizationFingerprint())
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                .build();
    }

    protected MaskedWalletRequest getMaskedWalletRequest(boolean isBillingAgreement,
            boolean shippingAddressRequired, boolean phoneNumberRequired) {
        MaskedWalletRequest.Builder maskedWalletRequestBuilder = MaskedWalletRequest.newBuilder()
                .setMerchantName(getMerchantName())
                .setCurrencyCode("USD")
                .setCart(mCart)
                .setIsBillingAgreement(isBillingAgreement)
                .setShippingAddressRequired(shippingAddressRequired)
                .setPhoneNumberRequired(phoneNumberRequired)
                .setPaymentMethodTokenizationParameters(getTokenizationParameters());

        if (mCart != null) {
            maskedWalletRequestBuilder.setEstimatedTotalPrice(mCart.getTotalPrice());
        }

        return maskedWalletRequestBuilder.build();
    }

    protected FullWalletRequest getFullWalletRequest(String googleTransactionId) {
        return FullWalletRequest.newBuilder()
                .setCart(mCart)
                .setGoogleTransactionId(googleTransactionId)
                .build();
    }

    protected void disconnect() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
    }

    /**
     * Creates a new {@link GoogleApiClient} if necessary and then connects it if necessary.
     * <b>Warning: </b> this method blocks and cannot be called on the main thread.
     *
     * @param context
     * @return A connected instance of {@link GoogleApiClient}
     * @throws UnexpectedException
     */
    private GoogleApiClient getConnectedApiClient(Context context) throws UnexpectedException {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                            .setEnvironment(getEnvironment())
                            .setTheme(WalletConstants.THEME_LIGHT)
                            .build())
                    .build();
        }

        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
            if (!connectionResult.isSuccess()) {
                throw new UnexpectedException("GoogleApiClient failed to connect with error code"
                        + connectionResult.getErrorCode());
            }
        }

        return mGoogleApiClient;
    }

    private int getEnvironment() {
        if(mConfiguration.getAndroidPay().getEnvironment() != null &&
                mConfiguration.getAndroidPay().getEnvironment().equals("production")) {
            return WalletConstants.ENVIRONMENT_PRODUCTION;
        } else {
            return WalletConstants.ENVIRONMENT_TEST;
        }
    }

    private String getMerchantName() {
        if (mConfiguration.getAndroidPay().getDisplayName() != null) {
            return mConfiguration.getAndroidPay().getDisplayName();
        } else {
            return "";
        }
    }
}
