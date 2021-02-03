package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface GooglePaymentOnActivityResultCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
