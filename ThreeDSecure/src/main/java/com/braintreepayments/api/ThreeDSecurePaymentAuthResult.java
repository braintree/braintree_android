package com.braintreepayments.api;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

public class ThreeDSecurePaymentAuthResult {

    private final String jwt;
    private final ValidateResponse validateResponse;
    private final ThreeDSecureInternalResult threeDSecureInternalResult;

    private final Exception error;

    ThreeDSecurePaymentAuthResult(ThreeDSecureInternalResult threeDSecureInternalResult, String jwt,
                                  ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.validateResponse = validateResponse;
        this.threeDSecureInternalResult = threeDSecureInternalResult;
        this.error = null;
    }

    ThreeDSecurePaymentAuthResult(Exception error) {
        this.error = error;
        this.jwt = null;
        this.validateResponse = null;
        this.threeDSecureInternalResult = null;
    }

    Exception getError() {
        return error;
    }

    ThreeDSecureInternalResult getThreeSecureResult() {
        return threeDSecureInternalResult;
    }

    ValidateResponse getValidateResponse() {
        return validateResponse;
    }

    String getJWT() {
        return jwt;
    }
}
