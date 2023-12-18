package com.braintreepayments.api;


import androidx.annotation.Nullable;

interface LocalPaymentInternalTokenizeCallback {

    void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error);
}
