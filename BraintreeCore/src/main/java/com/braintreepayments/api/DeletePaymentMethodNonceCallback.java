package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link PaymentMethodClient#deletePaymentMethod(Context, UntypedPaymentMethodNonce, DeletePaymentMethodNonceCallback)}.
 */
public interface DeletePaymentMethodNonceCallback {

    /**
     * @param deletedNonce {@link UntypedPaymentMethodNonce}
     * @param error an exception that occurred while deleting a payment method
     */
    void onResult(@Nullable UntypedPaymentMethodNonce deletedNonce, @Nullable Exception error);
}
