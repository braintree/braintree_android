package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.WalletConstants;

public class AndroidPay {

    private Configuration mConfiguration;
    private Cart mCart;

    protected AndroidPay(Configuration configuration, Cart cart) {
        mConfiguration = configuration;
        mCart = cart;
    }

    /**
     * Checks the given {@link Intent} to see if it contains a {@link com.google.android.gms.wallet.MaskedWallet}.
     *
     * @param intent The {@link Intent} to check.
     * @return {@code true} is the {@link Intent} contains a {@link WalletConstants#EXTRA_MASKED_WALLET},
     *         {@code false} otherwise.
     */
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
    public static boolean isFullWalletResponse(Intent intent) {
        return intent.hasExtra(WalletConstants.EXTRA_FULL_WALLET);
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

    private String getMerchantName() {
        if (mConfiguration.getAndroidPay().getDisplayName() != null) {
            return mConfiguration.getAndroidPay().getDisplayName();
        } else {
            return "";
        }
    }
}
