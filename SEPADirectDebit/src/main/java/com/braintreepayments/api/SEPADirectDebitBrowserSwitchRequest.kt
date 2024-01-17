package com.braintreepayments.api

/**
 * Request parameters associated with a [SEPADirectDebitPendingRequest.Started]
 */
class SEPADirectDebitBrowserSwitchRequest internal constructor(browserSwitchPendingRequest: BrowserSwitchPendingRequest.Started) {
    private var browserSwitchPendingRequest: BrowserSwitchPendingRequest.Started

    init {
        this.browserSwitchPendingRequest = browserSwitchPendingRequest
    }

    fun getBrowserSwitchPendingRequest(): BrowserSwitchPendingRequest.Started {
        return browserSwitchPendingRequest
    }
}