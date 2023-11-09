package com.braintreepayments.api;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

public class ThreeDSecurePaymentAuthResult {

    private final String jwt;
    private final ValidateResponse validateResponse;
    private final ThreeDSecureResult paymentAuthRequest;

    private final Exception error;

    ThreeDSecurePaymentAuthResult(ThreeDSecureResult paymentAuthRequest, String jwt,
                                  ValidateResponse validateResponse) {
        this.jwt = jwt;
        this.validateResponse = validateResponse;
        this.paymentAuthRequest = paymentAuthRequest;
        this.error = null;
    }

    ThreeDSecurePaymentAuthResult(Exception error) {
        this.error = error;
        this.jwt = null;
        this.validateResponse = null;
        this.paymentAuthRequest = null;
    }

    Exception getError() {
        return error;
    }

    ThreeDSecureResult getThreeSecureResult() {
        return paymentAuthRequest;
    }

    ValidateResponse getValidateResponse() {
        return validateResponse;
    }

    String getJWT() {
        return jwt;
    }
}
