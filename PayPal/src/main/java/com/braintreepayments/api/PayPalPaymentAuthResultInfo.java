package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Result received from the PayPal web flow through {@link PayPalTokenizeCallback}.
 * This result should be passed to
 * {@link PayPalClient#tokenize(PayPalPaymentAuthResultInfo, PayPalTokenizeCallback)}
 * to complete the PayPal payment flow.
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
