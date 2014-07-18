package com.braintreepayments.api.exceptions;

public class BraintreeSslException extends RuntimeException {
    public BraintreeSslException(Exception e) {
        super(e);
    }
}