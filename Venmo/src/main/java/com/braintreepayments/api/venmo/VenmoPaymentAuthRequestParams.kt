package com.braintreepayments.api.venmo

import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Used to request Venmo authentication via [VenmoLauncher.launch] )}
 */
data class VenmoPaymentAuthRequestParams internal constructor(
    // TODO: this should be internal and restricted to library scope but can't be until
    //  VenmoClientUnitTest is converted to Kotlin
    val browserSwitchOptions: BrowserSwitchOptions
)
