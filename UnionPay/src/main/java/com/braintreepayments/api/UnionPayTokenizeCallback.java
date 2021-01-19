package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.CardNonce;

public interface UnionPayTokenizeCallback {
    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
