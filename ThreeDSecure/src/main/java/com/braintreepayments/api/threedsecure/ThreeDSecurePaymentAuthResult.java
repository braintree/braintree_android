package com.braintreepayments.api.threedsecure;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

public class ThreeDSecurePaymentAuthResult {

    private final String jwt;
    private final ValidateResponse validateResponse;
    private final ThreeDSecureParams threeDSecureParams;

    private final Exception error;

    ThreeDSecurePaymentAuthResult(ThreeDSecureParams threeDSecureParams, String jwt,
                                  ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.validateResponse = validateResponse;
        this.threeDSecureParams = threeDSecureParams;
        this.error = null;
    }

    ThreeDSecurePaymentAuthResult(Exception error) {
        this.error = error;
        this.jwt = null;
        this.validateResponse = null;
        this.threeDSecureParams = null;
    }

    Exception getError() {
        return error;
    }

    ThreeDSecureParams getThreeSecureResult() {
        return threeDSecureParams;
    }

    ValidateResponse getValidateResponse() {
        return validateResponse;
    }

    String getJWT() {
        return jwt;
    }
}
