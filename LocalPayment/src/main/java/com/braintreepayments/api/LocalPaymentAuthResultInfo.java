package com.braintreepayments.api;


/**
 * Details of a {@link LocalPaymentAuthResult.Success}
 */
public class LocalPaymentAuthResultInfo {

    private final BrowserSwitchResultInfo browserSwitchResultInfo;

    LocalPaymentAuthResultInfo(BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    BrowserSwitchResultInfo getBrowserSwitchResultInfo() {
        return browserSwitchResultInfo;
    }
}