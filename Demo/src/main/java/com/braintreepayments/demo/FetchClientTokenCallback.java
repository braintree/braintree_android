package com.braintreepayments.demo;

import androidx.annotation.Nullable;

public interface FetchClientTokenCallback {
    void onResult(@Nullable String clientToken, @Nullable Exception error);
}
