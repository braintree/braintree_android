package com.braintreepayments.api.paypal;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BrowserSwitchFinalResult;

/**
 * Details of a {@link PayPalPaymentAuthResult.Success}
 */
public class PayPalPaymentAuthResultInfo {

    private final BrowserSwitchFinalResult.Success browserSwitchSuccess;

    PayPalPaymentAuthResultInfo(@NonNull BrowserSwitchFinalResult.Success browserSwitchSuccess) {
        this.browserSwitchSuccess = browserSwitchSuccess;
    }

    @NonNull
    BrowserSwitchFinalResult.Success getBrowserSwitchResult() {
        return browserSwitchSuccess;
    }
}
