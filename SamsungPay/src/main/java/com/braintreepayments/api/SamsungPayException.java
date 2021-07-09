package com.braintreepayments.api;

/**
 * Exception class representing SamsungPay errors.
 */
public class SamsungPayException extends Exception {

    private final int errorCode;

    SamsungPayException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    /**
     * @return {@link SamsungPayError}
     */
    public int getErrorCode() {
        return errorCode;
    }
}
