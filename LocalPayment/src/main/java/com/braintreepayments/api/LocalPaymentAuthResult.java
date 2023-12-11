package com.braintreepayments.api;

import android.content.Context;

/**
 * Result received from the local payment web flow through
 * {@link LocalPaymentInternalTokenizeCallback}. This result should be passed to
 * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResult, LocalPaymentInternalTokenizeCallback)}
 * to complete the local payment flow.
 */
public class LocalPaymentAuthResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    LocalPaymentAuthResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    LocalPaymentAuthResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }

}
