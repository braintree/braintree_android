package com.braintreepayments.api;

/**
 * Result received from the PayPal web flow through {@link PayPalBrowserSwitchResultCallback}.
 * This result should be passed to
 * {@link PayPalClient#onBrowserSwitchResult(PayPalBrowserSwitchResult, PayPalBrowserSwitchResultCallback)}
 * to complete the PayPal payment flow.
 */
public class PayPalBrowserSwitchResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    PayPalBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    PayPalBrowserSwitchResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }
}
