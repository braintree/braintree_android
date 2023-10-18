package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface LocalPaymentLauncherCallback {

    void onResult(@Nullable LocalPaymentBrowserSwitchResult localPaymentResult);
}
