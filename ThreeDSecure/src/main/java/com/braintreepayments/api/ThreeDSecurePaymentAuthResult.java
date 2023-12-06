package com.braintreepayments.api;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

public class ThreeDSecurePaymentAuthResult {

    private final String jwt;
    private final ValidateResponse validateResponse;
    private final ThreeDSecureBundledResult threeDSecureBundledResult;

    private final Exception error;

    ThreeDSecurePaymentAuthResult(ThreeDSecureBundledResult threeDSecureBundledResult, String jwt,
                                  ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.validateResponse = validateResponse;
        this.threeDSecureBundledResult = threeDSecureBundledResult;
        this.error = null;
    }

    ThreeDSecurePaymentAuthResult(Exception error) {
        this.error = error;
        this.jwt = null;
        this.validateResponse = null;
        this.threeDSecureBundledResult = null;
    }

    Exception getError() {
        return error;
    }

    ThreeDSecureBundledResult getThreeSecureResult() {
        return threeDSecureBundledResult;
    }

    ValidateResponse getValidateResponse() {
        return validateResponse;
    }

    String getJWT() {
        return jwt;
    }
}
