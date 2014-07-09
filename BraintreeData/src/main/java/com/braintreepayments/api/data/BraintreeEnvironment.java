package com.braintreepayments.api.data;

public enum BraintreeEnvironment {
    QA("https://assets.qa.braintreegateway.com/data/logo.htm"),
    SANDBOX("https://assets.braintreegateway.com/sandbox/data/logo.htm"),
    PRODUCTION("https://assets.braintreegateway.com/data/logo.htm");

    private static final String BRAINTREE_MERCHANT_ID = "600000";

    private String mUrl;

    private BraintreeEnvironment(String url) {
        mUrl = url;
    }

    public String getMerchantId() {
        return BRAINTREE_MERCHANT_ID;
    }

    public String getCollectorUrl() {
        return mUrl;
    }
}