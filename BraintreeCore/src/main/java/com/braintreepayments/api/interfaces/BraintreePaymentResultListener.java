package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.BraintreePaymentResult;

/**
 * Interface that defines a callback to be used when a {@link BraintreePaymentResult} is created.
 */
public interface BraintreePaymentResultListener extends BraintreeListener {
    void onBraintreePaymentResult(BraintreePaymentResult result);
}
