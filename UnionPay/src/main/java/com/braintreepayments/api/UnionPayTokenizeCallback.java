package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface UnionPayTokenizeCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
