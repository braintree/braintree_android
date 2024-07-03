package com.braintreepayments.api.sepadirectdebit;

import com.braintreepayments.api.BrowserSwitchFinalResult;

/**
 * Details of a {@link SEPADirectDebitPaymentAuthResult.Success}
 */
public class SEPADirectDebitPaymentAuthResultInfo {

    private BrowserSwitchFinalResult.Success browserSwitchSuccess;

    SEPADirectDebitPaymentAuthResultInfo(BrowserSwitchFinalResult.Success browserSwitchSuccess) {
        this.browserSwitchSuccess = browserSwitchSuccess;
    }

    BrowserSwitchFinalResult.Success getBrowserSwitchSuccess() {
        return browserSwitchSuccess;
    }

}
