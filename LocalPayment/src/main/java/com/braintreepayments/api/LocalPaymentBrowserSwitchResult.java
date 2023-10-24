package com.braintreepayments.api;

import android.content.Context;

/**
 * Result received from the local payment web flow through
 * {@link LocalPaymentBrowserSwitchResultCallback}. This result should be passed to
 * {@link LocalPaymentClient#onBrowserSwitchResult(Context, LocalPaymentBrowserSwitchResult, LocalPaymentBrowserSwitchResultCallback)}
 * to complete the local payment flow.
 */
public class LocalPaymentBrowserSwitchResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    LocalPaymentBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    LocalPaymentBrowserSwitchResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }

}
