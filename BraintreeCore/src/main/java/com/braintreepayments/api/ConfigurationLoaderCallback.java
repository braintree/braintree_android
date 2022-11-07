package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface ConfigurationLoaderCallback {

    void onResult(@Nullable ConfigurationLoaderResult result, @Nullable Exception error);
}
