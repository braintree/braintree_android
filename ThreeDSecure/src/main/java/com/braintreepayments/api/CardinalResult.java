package com.braintreepayments.api;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

class CardinalResult {

    private final String jwt;
    private final ValidateResponse validateResponse;
    private final ThreeDSecureResult threeDSecureResult;

    private final Exception error;

    CardinalResult(ThreeDSecureResult threeDSecureResult, String jwt, ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.validateResponse = validateResponse;
        this.threeDSecureResult = threeDSecureResult;
        this.error = null;
    }

    CardinalResult(Exception error) {
        this.error = error;
        this.jwt = null;
        this.validateResponse = null;
        this.threeDSecureResult = null;
    }

    Exception getError() {
        return error;
    }

    ThreeDSecureResult getThreeSecureResult() {
        return threeDSecureResult;
    }

    ValidateResponse getValidateResponse() {
        return validateResponse;
    }

    String getJWT() {
        return jwt;
    }
}
