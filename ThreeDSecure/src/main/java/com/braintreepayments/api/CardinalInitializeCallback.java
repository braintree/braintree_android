package com.braintreepayments.api;

public interface CardinalInitializeCallback {
    void onResult(String consumerSessionId, Exception error);
}
