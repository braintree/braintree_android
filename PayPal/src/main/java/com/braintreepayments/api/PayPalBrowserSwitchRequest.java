package com.braintreepayments.api;

/**
 * Request parameters associated with a {@link PayPalPendingRequest.Started}
 */
public class PayPalBrowserSwitchRequest {

    BrowserSwitchPendingRequest.Started browserSwitchPendingRequest;

    PayPalBrowserSwitchRequest(BrowserSwitchPendingRequest.Started browserSwitchPendingRequest) {
        this.browserSwitchPendingRequest = browserSwitchPendingRequest;
    }

    BrowserSwitchPendingRequest.Started getBrowserSwitchRequest() {
        return browserSwitchPendingRequest;
    }
}