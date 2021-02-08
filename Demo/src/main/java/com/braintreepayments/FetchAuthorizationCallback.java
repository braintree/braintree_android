package com.braintreepayments;

import androidx.annotation.Nullable;

public interface FetchAuthorizationCallback {
    void onResult(@Nullable String authorization, @Nullable Exception error);
}
