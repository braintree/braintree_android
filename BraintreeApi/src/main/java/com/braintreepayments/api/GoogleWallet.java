package com.braintreepayments.api;

import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;

public class GoogleWallet {

    private ClientToken mClientToken;
    private Configuration mConfiguration;

    protected GoogleWallet(ClientToken clientToken, Configuration configuration) {
        mClientToken = clientToken;
        mConfiguration = configuration;
    }

    protected MaskedWalletRequest getMaskedWalletRequest() {
        PaymentMethodTokenizationParameters paymentMethodTokenizationParameters =
                PaymentMethodTokenizationParameters.newBuilder()
                        .setPaymentMethodTokenizationType(
                                PaymentMethodTokenizationType.PAYMENT_GATEWAY)
                        .addParameter("gateway", "braintree")
                        .addParameter("braintree:merchantId", mConfiguration.getMerchantId())
                        .addParameter("braintree:authorizationFingerprint",
                                mClientToken.getAuthorizationFingerprint())
                        .addParameter("braintree:apiVersion", "v1")
                        .addParameter("braintree:sdkVersion", BuildConfig.VERSION_NAME)
                        .build();

        return MaskedWalletRequest.newBuilder()
                .setMerchantName("Braintree Demo")
                .setCurrencyCode("USD")
                .setEstimatedTotalPrice("150.00")
                .setPaymentMethodTokenizationParameters(paymentMethodTokenizationParameters)
                .build();
    }

    protected FullWalletRequest getFullWalletRequest(String googleTransactionId) {
        return FullWalletRequest.newBuilder()
                .setCart(Cart.newBuilder()
                        .setCurrencyCode("USD")
                        .setTotalPrice("150.00")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode("USD")
                                .setDescription("Description")
                                .setQuantity("1")
                                .setUnitPrice("150.00")
                                .setTotalPrice("150.00")
                                .build())
                        .build())
                .setGoogleTransactionId(googleTransactionId)
                .build();
    }
}
