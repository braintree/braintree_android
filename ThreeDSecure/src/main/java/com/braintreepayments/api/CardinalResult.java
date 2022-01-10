package com.braintreepayments.api;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

public class CardinalResult {

    private final String jwt;
    private final ThreeDSecureResult threeDSecureResult;
    private final ValidateResponse validateResponse;

    public CardinalResult(ThreeDSecureResult threeDSecureResult, String jwt, ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.threeDSecureResult = threeDSecureResult;
        this.validateResponse = validateResponse;
    }

    public String getJwt() {
        return jwt;
    }

    public ThreeDSecureResult getThreeDSecureResult() {
        return threeDSecureResult;
    }

    public ValidateResponse getValidateResponse() {
        return validateResponse;
    }
}
