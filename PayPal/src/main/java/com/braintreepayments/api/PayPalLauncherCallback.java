package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface PayPalLauncherCallback {

    void onResult(@NonNull PayPalBrowserSwitchResult payPalBrowserSwitchResult);
}
