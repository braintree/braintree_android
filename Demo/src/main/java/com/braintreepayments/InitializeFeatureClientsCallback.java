package com.braintreepayments;

import androidx.annotation.Nullable;

public interface InitializeFeatureClientsCallback {
    void onResult(@Nullable Exception error);
}
