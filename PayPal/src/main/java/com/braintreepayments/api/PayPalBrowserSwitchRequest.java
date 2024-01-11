package com.braintreepayments.api;

/**
 * Request parameters associated with a {@link PayPalPendingRequest.Started}
 */
public class PayPalBrowserSwitchRequest {

    BrowserSwitchRequest browserSwitchRequest;

    PayPalBrowserSwitchRequest(BrowserSwitchRequest browserSwitchRequest) {
        this.browserSwitchRequest = browserSwitchRequest;
    }

    BrowserSwitchRequest getBrowserSwitchRequest() {
        return browserSwitchRequest;
    }
}
