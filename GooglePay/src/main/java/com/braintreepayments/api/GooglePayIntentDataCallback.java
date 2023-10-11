package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface GooglePayIntentDataCallback {

    void onGooglePayIntentData(@Nullable GooglePayIntentData googlePayIntentData,
                               @Nullable Exception error);
}
