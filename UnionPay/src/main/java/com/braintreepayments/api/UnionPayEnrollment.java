package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Union Pay enrollment information
 */
public class UnionPayEnrollment {

    private final String id;
    private final boolean smsCodeRequired;

    UnionPayEnrollment(String id, boolean smsCodeRequired) {
        this.id = id;
        this.smsCodeRequired = smsCodeRequired;
    }

    /**
     * @return the UnionPay enrollment ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * @return true if sms code is required, false otherwise
     */
    public boolean isSmsCodeRequired() {
        return smsCodeRequired;
    }
}
