package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.util.List;

/**
 * Interface that defines callbacks to be called when existing {@link PaymentMethodNonce}s are fetched.
 */
public interface PaymentMethodNoncesUpdatedListener extends BraintreeListener {

    /**
     * {@link #onPaymentMethodNoncesUpdated(List)} will be called with a list of {@link PaymentMethodNonce}s
     * as a callback when
     * {@link com.braintreepayments.api.TokenizationClient#getPaymentMethodNonces(BraintreeFragment)}
     * is called.
     *
     * @param paymentMethodNonces the {@link List} of {@link PaymentMethodNonce}s.
     */
    void onPaymentMethodNoncesUpdated(List<PaymentMethodNonce> paymentMethodNonces);
}
