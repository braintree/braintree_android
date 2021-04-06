package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link PaymentMethodClient#deletePaymentMethod(Context, PaymentMethodNonce, DeletePaymentMethodNonceCallback)}.
 */
public interface DeletePaymentMethodNonceCallback {

    /**
     * @param deletedNonce {@link PaymentMethodNonce}
     * @param error an exception that occurred while deleting a payment method
     */
    void onResult(@Nullable PaymentMethodNonce deletedNonce, @Nullable Exception error);
}
