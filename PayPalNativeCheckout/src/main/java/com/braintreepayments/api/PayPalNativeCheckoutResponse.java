package com.braintreepayments.api;

class PayPalNativeCheckoutResponse {

    private String clientMetadataId;
    private String pairingId;
    private final PayPalNativeRequest payPalRequest;

    PayPalNativeCheckoutResponse(PayPalNativeRequest payPalRequest) {
        this.payPalRequest = payPalRequest;
    }

    String getClientMetadataId() {
        return clientMetadataId;
    }

    PayPalNativeCheckoutResponse clientMetadataId(String value) {
        clientMetadataId = value;
        return this;
    }

    String getIntent() {
        if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
            return ((PayPalNativeCheckoutRequest) payPalRequest).getIntent();
        }
        return null;
    }

    boolean isBillingAgreement() {
        return payPalRequest instanceof PayPalNativeCheckoutVaultRequest;
    }

    String getMerchantAccountId() {
        return payPalRequest.getMerchantAccountId();
    }

    String getPairingId() {
        return pairingId;
    }

    PayPalNativeCheckoutResponse pairingId(String value) {
        pairingId = value;
        return this;
    }
}
