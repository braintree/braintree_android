package com.braintreepayments.api;

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
