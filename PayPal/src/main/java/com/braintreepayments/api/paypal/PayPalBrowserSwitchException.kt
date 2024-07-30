package com.braintreepayments.api.paypal

/**
 * Exception for whenever the browser has returned an 'error' in its response.
 */
class PayPalBrowserSwitchException internal constructor(
    detailMessage: String
) : Exception(detailMessage)
