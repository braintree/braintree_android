package com.braintreepayments.api;

class PayPalNativeCheckoutResponse {

    private String approvalUrl;
    private String clientMetadataId;
    private String pairingId;
    private final PayPalNativeRequest payPalRequest;

    private String successUrl;

    PayPalNativeCheckoutResponse(PayPalNativeRequest payPalRequest) {
        this.payPalRequest = payPalRequest;
    }

    String getApprovalUrl() {
        return approvalUrl;
    }

    PayPalNativeCheckoutResponse approvalUrl(String value) {
        approvalUrl = value;
        return this;
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

    String getUserAction() {
        if (payPalRequest instanceof PayPalNativeCheckoutRequest) {
            return ((PayPalNativeCheckoutRequest) payPalRequest).getUserAction();
        }
        return "";
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

    String getSuccessUrl() {
        return successUrl;
    }

    PayPalNativeCheckoutResponse successUrl(String value) {
        successUrl = value;
        return this;
    }
}
