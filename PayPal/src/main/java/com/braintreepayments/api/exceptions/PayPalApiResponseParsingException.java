package com.braintreepayments.api.exceptions;

public class PayPalApiResponseParsingException extends Exception {

    public PayPalApiResponseParsingException(Throwable throwable) {
        super(throwable);
    }
}
