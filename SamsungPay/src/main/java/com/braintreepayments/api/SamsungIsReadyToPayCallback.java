package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface SamsungIsReadyToPayCallback {
    void onResult(boolean isReadyToPay, @Nullable Exception error);
}
