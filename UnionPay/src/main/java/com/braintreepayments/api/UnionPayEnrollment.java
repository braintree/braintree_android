package com.braintreepayments.api;

public class UnionPayEnrollment {

    private final String id;
    private final boolean smsCodeRequired;

    UnionPayEnrollment(String id, boolean smsCodeRequired) {
        this.id = id;
        this.smsCodeRequired = smsCodeRequired;
    }

    public String getId() {
        return id;
    }

    public boolean isSmsCodeRequired() {
        return smsCodeRequired;
    }
}
