package com.braintreepayments.api;

// TODO: - Docstring
public class SEPADirectDebitBrowserSwitchResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    SEPADirectDebitBrowserSwitchResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    SEPADirectDebitBrowserSwitchResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }
}