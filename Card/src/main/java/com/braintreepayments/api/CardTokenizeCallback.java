package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.CardNonce;

public interface CardTokenizeCallback {

    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
