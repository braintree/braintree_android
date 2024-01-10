package com.braintreepayments.api;

public class PayPalBrowserSwitchRequest {


    BrowserSwitchRequest browserSwitchRequest;

    PayPalBrowserSwitchRequest(BrowserSwitchRequest browserSwitchRequest) {
        this.browserSwitchRequest = browserSwitchRequest;
    }

    BrowserSwitchRequest getBrowserSwitchRequest() {
        return browserSwitchRequest;
    }
}
