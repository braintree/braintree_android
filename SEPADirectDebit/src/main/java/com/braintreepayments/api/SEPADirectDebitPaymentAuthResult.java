package com.braintreepayments.api;

/**
 * Result received from the SEPA mandate web flow through {@link SEPADirectDebitTokenizeCallback}.
 * This result should be passed to
 * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitPaymentAuthResult, SEPADirectDebitTokenizeCallback)} )}
 * to complete the SEPA mandate flow.
 */
public class SEPADirectDebitPaymentAuthResult {

    private BrowserSwitchResult browserSwitchResult;
    private Exception error;

    SEPADirectDebitPaymentAuthResult(BrowserSwitchResult browserSwitchResult) {
        this.browserSwitchResult = browserSwitchResult;
    }

    SEPADirectDebitPaymentAuthResult(Exception error) {
        this.error = error;
    }

    BrowserSwitchResult getBrowserSwitchResult() {
        return browserSwitchResult;
    }

    Exception getError() {
        return error;
    }
}
