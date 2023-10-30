package com.braintreepayments.api;

public class SEPADirectDebitResponse {

    private BrowserSwitchOptions browserSwitchOptions;

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    void setBrowserSwitchOptions(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }
}
