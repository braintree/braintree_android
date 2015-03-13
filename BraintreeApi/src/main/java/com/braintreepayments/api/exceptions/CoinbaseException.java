package com.braintreepayments.api.exceptions;

public class CoinbaseException extends BraintreeException {

    public CoinbaseException(String errorMessage) {
        super(errorMessage);
    }
}
