package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams(
    var payPalContextId: String? = null,
    var linkType: String? = null,
    var isVaultRequest: Boolean = false,
    var startTime: Long? = null,
    var endTime: Long? = null,
    var endpoint: String? = null
) {
    // TODO: this is a convenience constructor for Java; remove after Kotlin migration is complete
    constructor() : this(null, null, false, null, null, null)
}
