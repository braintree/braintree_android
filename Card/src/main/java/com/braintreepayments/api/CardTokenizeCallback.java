package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface CardTokenizeCallback {
    // TODO: Change parameter to CardNonce when API is finalized
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
