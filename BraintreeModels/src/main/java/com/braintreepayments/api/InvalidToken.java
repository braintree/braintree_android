package com.braintreepayments.api;

class InvalidToken extends Authorization {

    private final String errorMessage;

    InvalidToken(String rawValue, String errorMessage) {
        super(rawValue);
        this.errorMessage = errorMessage;
    }

    @Override
    String getConfigUrl() {
        return null;
    }

    @Override
    String getBearer() {
        return null;
    }

    String getErrorMessage() {
        return errorMessage;
    }
}
