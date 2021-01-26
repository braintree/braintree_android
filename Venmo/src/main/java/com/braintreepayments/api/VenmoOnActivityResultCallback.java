package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface VenmoOnActivityResultCallback {

    void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error);
}
