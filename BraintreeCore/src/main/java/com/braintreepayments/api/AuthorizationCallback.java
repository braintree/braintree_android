package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface AuthorizationCallback {
    void onAuthorizationResult(@Nullable Authorization authorization, @Nullable Exception error);
}
