package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface UnionPayEnrollCallback {
    void onResult(@Nullable UnionPayEnrollment enrollment, @Nullable Exception error);
}
