package com.braintreepayments.demo;

public class TransactionRequest {

    private final String paymentMethodNonce;
    private final String merchantAccountId;

    private Boolean threeDSecureRequired;

    public TransactionRequest(String paymentMethodNonce) {
        this(paymentMethodNonce, null, null);
    }

    public TransactionRequest(String paymentMethodNonce, String merchantAccountId) {
        this(paymentMethodNonce, merchantAccountId, null);
    }

    public TransactionRequest(String paymentMethodNonce, String merchantAccountId, Boolean threeDSecureRequired) {
        this.paymentMethodNonce = paymentMethodNonce;
        this.merchantAccountId = merchantAccountId;
        this.threeDSecureRequired = threeDSecureRequired;
    }

    public String getPaymentMethodNonce() {
        return paymentMethodNonce;
    }

    public String getMerchantAccountId() {
        return merchantAccountId;
    }

    public boolean isThreeDSecureRequired() {
        return threeDSecureRequired;
    }

    public void setThreeDSecureRequired(Boolean threeDSecureRequired) {
        this.threeDSecureRequired = threeDSecureRequired;
    }
}
