package com.braintreepayments.api;

import android.content.Context;

/**
 * Result received from the local payment web flow through
 * {@link LocalPaymentInternalTokenizeCallback}. This result should be passed to
 * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResultInfo, LocalPaymentInternalTokenizeCallback)}
 * to complete the local payment flow.
 */
public class LocalPaymentAuthResultInfo {

    private final BrowserSwitchResultInfo browserSwitchResultInfo;

    LocalPaymentAuthResultInfo(BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    BrowserSwitchResultInfo getBrowserSwitchResultInfo() {
        return browserSwitchResultInfo;
    }
}
