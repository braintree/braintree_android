package com.braintreepayments.api;

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
