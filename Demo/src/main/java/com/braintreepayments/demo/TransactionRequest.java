package com.braintreepayments.demo;

public class TransactionRequest {

    private final String amount;
    private final String paymentMethodNonce;
    private final String merchantAccountId;

    private Boolean threeDSecureRequired;

    public TransactionRequest(String amount, String paymentMethodNonce) {
        this(amount, paymentMethodNonce, null, null);
    }

    public TransactionRequest(String amount, String paymentMethodNonce, String merchantAccountId) {
        this(amount, paymentMethodNonce, merchantAccountId, null);
    }

    public TransactionRequest(String amount, String paymentMethodNonce, String merchantAccountId, Boolean threeDSecureRequired) {
        this.amount = amount;
        this.paymentMethodNonce = paymentMethodNonce;
        this.merchantAccountId = merchantAccountId;
        this.threeDSecureRequired = threeDSecureRequired;
    }

    public String getAmount() {
        return amount;
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
