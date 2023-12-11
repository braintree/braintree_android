package com.braintreepayments.api;


import androidx.annotation.Nullable;

interface LocalPaymentInternalCallback {

    void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error);
}
