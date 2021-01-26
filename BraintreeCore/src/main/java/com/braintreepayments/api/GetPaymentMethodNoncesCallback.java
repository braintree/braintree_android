package com.braintreepayments.api;

import androidx.annotation.Nullable;

import java.util.List;

public interface GetPaymentMethodNoncesCallback {
    void onResult(@Nullable List<PaymentMethodNonce> paymentMethodNonceList, @Nullable Exception error);
}
