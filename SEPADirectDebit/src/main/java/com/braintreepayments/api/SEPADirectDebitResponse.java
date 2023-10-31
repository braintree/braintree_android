package com.braintreepayments.api;

public class SEPADirectDebitResponse {

    private BrowserSwitchOptions browserSwitchOptions;
    private SEPADirectDebitNonce sepaDirectDebitNonce;

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }

    void setBrowserSwitchOptions(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }

    void setNonce(SEPADirectDebitNonce sepaDirectDebitNonce) {
        this.sepaDirectDebitNonce = sepaDirectDebitNonce;
    }

    public SEPADirectDebitNonce getNonce() {
        return this.sepaDirectDebitNonce;
    }
}
