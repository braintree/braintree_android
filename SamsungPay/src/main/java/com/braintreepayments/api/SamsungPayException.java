package com.braintreepayments.api;

public class SamsungPayException extends Exception {

    private final int errorCode;

    SamsungPayException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
