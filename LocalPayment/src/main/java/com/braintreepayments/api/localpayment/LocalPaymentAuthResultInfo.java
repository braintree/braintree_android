package com.braintreepayments.api.localpayment;


import com.braintreepayments.api.BrowserSwitchFinalResult;

/**
 * Details of a {@link LocalPaymentAuthResult.Success}
 */
public class LocalPaymentAuthResultInfo {

    private final BrowserSwitchFinalResult.Success browserSwitchSuccess;

    LocalPaymentAuthResultInfo(BrowserSwitchFinalResult.Success browserSwitchSuccess) {
        this.browserSwitchSuccess = browserSwitchSuccess;
    }

    BrowserSwitchFinalResult.Success getBrowserSwitchResultInfo() {
        return browserSwitchSuccess;
    }
}
