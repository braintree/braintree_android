package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Details of a {@link PayPalPaymentAuthResult.Success}
 */
public class PayPalPaymentAuthResultInfo {

    private final BrowserSwitchResultInfo browserSwitchResultInfo;

    PayPalPaymentAuthResultInfo(@NonNull BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    @NonNull
    BrowserSwitchResultInfo getBrowserSwitchResult() {
        return browserSwitchResultInfo;
    }
}
