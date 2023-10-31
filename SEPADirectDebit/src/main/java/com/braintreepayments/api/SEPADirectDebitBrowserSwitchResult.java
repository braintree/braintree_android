package com.braintreepayments.api;

/**
 * Result received from the SEPA mandate web flow through {@link SEPADirectDebitBrowserSwitchResultCallback}.
 * This result should be passed to
 * {@link SEPADirectDebitClient#onBrowserSwitchResult(SEPADirectDebitBrowserSwitchResult, SEPADirectDebitBrowserSwitchResultCallback)} )}
 * to complete the SEPA mandate flow.
 */
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
