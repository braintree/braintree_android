package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface ConfigurationCallback {
    void onResult(@Nullable Configuration configuration, @Nullable Exception error);
}
