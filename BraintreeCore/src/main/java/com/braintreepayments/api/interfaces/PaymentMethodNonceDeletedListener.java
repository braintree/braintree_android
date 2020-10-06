package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PaymentMethodNonce;

/**
 * Interface that defines callbacks to be called after a {@link PaymentMethodNonce} is deleted.
 */
public interface PaymentMethodNonceDeletedListener extends BraintreeListener {
    void onPaymentMethodNonceDeleted(PaymentMethodNonce paymentMethodNonce);
}