package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface CreateMandateCallback {
    void onResult(@Nullable CreateMandateResult result, @Nullable Exception error);
}
