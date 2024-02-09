package com.braintreepayments.api;

/**
 * Details of a {@link SEPADirectDebitPaymentAuthResult.Success}
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
