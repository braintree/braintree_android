package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface SamsungPayIsReadyToPayCallback {
    void onResult(boolean isReadyToPay, @Nullable Exception error);
}
