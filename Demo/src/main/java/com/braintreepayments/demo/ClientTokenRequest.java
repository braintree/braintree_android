package com.braintreepayments.demo;

public class ClientTokenRequest {

    private final String customerId;
    private final String merchantAccountId;

    public ClientTokenRequest() {
        this(null, null);
    }

    public ClientTokenRequest(String customerId) {
        this(customerId, null);
    }

    public ClientTokenRequest(String customerId, String merchantAccountId) {
        this.customerId = customerId;
        this.merchantAccountId = merchantAccountId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMerchantAccountId() {
        return merchantAccountId;
    }
}
