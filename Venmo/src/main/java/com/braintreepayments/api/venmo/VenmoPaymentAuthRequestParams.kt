package com.braintreepayments.api.venmo

import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Used to request Venmo authentication via [VenmoLauncher.launch] )}
 */
class VenmoPaymentAuthRequestParams internal constructor(
    val browserSwitchOptions: BrowserSwitchOptions
)
