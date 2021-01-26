package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface CardTokenizeCallback {

    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
