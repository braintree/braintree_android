package com.braintreepayments.api;

class PayPalResponse {

    private String approvalUrl;
    private String clientMetadataId;
    private String pairingId;
    private final PayPalRequest payPalRequest;

    private String successUrl;

    PayPalResponse(PayPalRequest payPalRequest) {
        this.payPalRequest = payPalRequest;
    }

    String getApprovalUrl() {
        return approvalUrl;
    }

    PayPalResponse approvalUrl(String value) {
        approvalUrl = value;
        return this;
    }

    String getClientMetadataId() {
        return clientMetadataId;
    }

    PayPalResponse clientMetadataId(String value) {
        clientMetadataId = value;
        return this;
    }

    String getIntent() {
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            return ((PayPalCheckoutRequest) payPalRequest).getIntent();
        }
        return null;
    }

    String getUserAction() {
        if (payPalRequest instanceof  PayPalCheckoutRequest) {
            return ((PayPalCheckoutRequest) payPalRequest).getUserAction();
        }
        return "";
    }

    boolean isBillingAgreement() {
        return payPalRequest instanceof PayPalVaultRequest;
    }

    String getMerchantAccountId() {
        return payPalRequest.getMerchantAccountId();
    }

    String getPairingId() {
        return pairingId;
    }

    PayPalResponse pairingId(String value) {
        pairingId = value;
        return this;
    }

    String getSuccessUrl() {
        return successUrl;
    }

    PayPalResponse successUrl(String value) {
        successUrl = value;
        return this;
    }
}
