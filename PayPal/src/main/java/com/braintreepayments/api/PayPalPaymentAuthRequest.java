package com.braintreepayments.api;

public class PayPalPaymentAuthRequest {

    private String approvalUrl;
    private String clientMetadataId;
    private String pairingId;
    private final PayPalRequest payPalRequest;

    private String successUrl;

    private BrowserSwitchOptions browserSwitchOptions;

    PayPalPaymentAuthRequest(PayPalRequest payPalRequest) {
        this.payPalRequest = payPalRequest;
    }

    String getApprovalUrl() {
        return approvalUrl;
    }

    PayPalPaymentAuthRequest approvalUrl(String value) {
        approvalUrl = value;
        return this;
    }

    String getClientMetadataId() {
        return clientMetadataId;
    }

    PayPalPaymentAuthRequest clientMetadataId(String value) {
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
        if (payPalRequest instanceof PayPalCheckoutRequest) {
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

    PayPalPaymentAuthRequest pairingId(String value) {
        pairingId = value;
        return this;
    }

    String getSuccessUrl() {
        return successUrl;
    }

    PayPalPaymentAuthRequest successUrl(String value) {
        successUrl = value;
        return this;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    void setBrowserSwitchOptions(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }
}
