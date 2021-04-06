package com.braintreepayments.api;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Callback for receiving result of
 * {@link PaymentMethodClient#getPaymentMethodNonces(GetPaymentMethodNoncesCallback)} and
 * {@link PaymentMethodClient#getPaymentMethodNonces(boolean, GetPaymentMethodNoncesCallback)}.
 */
public interface GetPaymentMethodNoncesCallback {

    /**
     * @param braintreeNonceList {@link BraintreeNonce} list
     * @param error an exception that occurred while fetching payment method nonces
     */
    void onResult(@Nullable List<BraintreeNonce> braintreeNonceList, @Nullable Exception error);
}
