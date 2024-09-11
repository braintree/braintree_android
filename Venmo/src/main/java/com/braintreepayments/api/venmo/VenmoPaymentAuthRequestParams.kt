package com.braintreepayments.api.venmo

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Used to request Venmo authentication via [VenmoLauncher.launch] )}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class VenmoPaymentAuthRequestParams internal constructor(
    val browserSwitchOptions: BrowserSwitchOptions
)
