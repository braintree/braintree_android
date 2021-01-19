package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.VenmoAccountNonce;

public interface VenmoOnActivityResultCallback {

    void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error);
}
