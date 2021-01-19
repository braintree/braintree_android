package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface VenmoAuthorizeAccountCallback {

    void onResult(@Nullable Exception error);
}
