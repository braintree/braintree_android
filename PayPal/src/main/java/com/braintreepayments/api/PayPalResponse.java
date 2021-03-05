package com.braintreepayments.api;

class PayPalResponse {

    private String approvalUrl;
    private String clientMetadataId;
    private String intent;

    private boolean isBillingAgreement;
    private String merchantAccountId;
    private String pairingId;

    private String successUrl;

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
        return intent;
    }

    PayPalResponse intent(String value) {
        intent = value;
        return this;
    }

    boolean isBillingAgreement() {
        return isBillingAgreement;
    }

    PayPalResponse isBillingAgreement(boolean value) {
        isBillingAgreement = value;
        return this;
    }

    String getMerchantAccountId() {
        return merchantAccountId;
    }

    PayPalResponse merchantAccountId(String value) {
        merchantAccountId = value;
        return this;
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
