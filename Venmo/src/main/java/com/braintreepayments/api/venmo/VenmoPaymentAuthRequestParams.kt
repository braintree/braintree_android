package com.braintreepayments.api.venmo

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.Authorization

/**
 * Used to request Venmo authentication via [VenmoLauncher.launch] )}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class VenmoPaymentAuthRequestParams internal constructor(
    val browserSwitchOptions: BrowserSwitchOptions
) {
    // body property: keeps the credential out of the data class generated toString/equals/copy
    internal var authorization: Authorization? = null
}
