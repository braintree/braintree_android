package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.Configuration;

public interface ConfigurationCallback {
    void onResult(@Nullable Configuration configuration, @Nullable Exception error);
}
