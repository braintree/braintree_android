package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface ConfigurationLoaderCallback {

    void onResult(@Nullable Configuration result, @Nullable Exception error);
}
