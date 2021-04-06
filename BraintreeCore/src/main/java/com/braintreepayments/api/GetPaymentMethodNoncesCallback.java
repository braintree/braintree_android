package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Callback for receiving result of
 * {@link PaymentMethodClient#getPaymentMethodNonces(Context, GetPaymentMethodNoncesCallback)} and
 * {@link PaymentMethodClient#getPaymentMethodNonces(boolean, GetPaymentMethodNoncesCallback)}.
 */
public interface GetPaymentMethodNoncesCallback {

    /**
     * @param paymentMethodNonceList {@link PaymentMethodNonce} list
     * @param error an exception that occurred while fetching payment method nonces
     */
    void onResult(@Nullable List<PaymentMethodNonce> paymentMethodNonceList, @Nullable Exception error);
}
