package com.braintreepayments.api.paypal;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BrowserSwitchResultInfo;

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
