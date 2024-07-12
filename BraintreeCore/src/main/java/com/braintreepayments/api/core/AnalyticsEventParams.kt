package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams(
    var payPalContextId: String?,
    var linkType: String?,
    var isVaultRequest: Boolean,
) {
    // TODO: this is a convenience constructor for Java; remove after Kotlin migration is complete
    constructor() : this(null, null, false)
}
