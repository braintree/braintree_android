package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface UnionPayTokenizeCallback {
    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
