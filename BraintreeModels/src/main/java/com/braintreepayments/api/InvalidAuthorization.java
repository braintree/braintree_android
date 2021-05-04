package com.braintreepayments.api;

class InvalidAuthorization extends Authorization {

    private final String errorMessage;

    InvalidAuthorization(String rawValue, String errorMessage) {
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
