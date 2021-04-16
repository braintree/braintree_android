package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link PaymentMethodClient#deletePaymentMethod(Context, BraintreeNonce, DeletePaymentMethodNonceCallback)}.
 */
public interface DeletePaymentMethodNonceCallback {

    /**
     * @param deletedNonce {@link BraintreeNonce}
     * @param error an exception that occurred while deleting a payment method
     */
    void onResult(@Nullable BraintreeNonce deletedNonce, @Nullable Exception error);
}
