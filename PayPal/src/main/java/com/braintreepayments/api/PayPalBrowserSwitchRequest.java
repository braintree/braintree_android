package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Request parameters associated with a {@link PayPalPendingRequest.Started}
 */
public class PayPalBrowserSwitchRequest {

    BrowserSwitchPendingRequest.Started browserSwitchPendingRequest;

    PayPalBrowserSwitchRequest(@NonNull BrowserSwitchPendingRequest.Started browserSwitchPendingRequest) {
        this.browserSwitchPendingRequest = browserSwitchPendingRequest;
    }

    @NonNull
    BrowserSwitchPendingRequest.Started getBrowserSwitchPendingRequest() {
        return browserSwitchPendingRequest;
    }
}