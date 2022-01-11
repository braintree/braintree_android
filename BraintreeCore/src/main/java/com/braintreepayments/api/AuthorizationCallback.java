package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface AuthorizationCallback {
    void onAuthorization(@Nullable Authorization authorization, @Nullable Exception error);
}
