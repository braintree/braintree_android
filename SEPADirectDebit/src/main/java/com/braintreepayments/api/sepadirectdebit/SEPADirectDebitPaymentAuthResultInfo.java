package com.braintreepayments.api.sepadirectdebit;

import com.braintreepayments.api.BrowserSwitchResultInfo;

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
