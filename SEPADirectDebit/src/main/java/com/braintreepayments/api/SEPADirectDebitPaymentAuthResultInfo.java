package com.braintreepayments.api;

/**
 * Result received from the SEPA mandate web flow through {@link SEPADirectDebitInternalTokenizeCallback}.
 * This result should be passed to
 * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitPaymentAuthResult.Success, SEPADirectDebitInternalTokenizeCallback)} )}
 * to complete the SEPA mandate flow.
 */
public class SEPADirectDebitPaymentAuthResultInfo {

    private BrowserSwitchResultInfo browserSwitchResultInfo;

    SEPADirectDebitPaymentAuthResultInfo(BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    BrowserSwitchResultInfo getBrowserSwitchResultInfo() {
        return browserSwitchResultInfo;
    }

}
