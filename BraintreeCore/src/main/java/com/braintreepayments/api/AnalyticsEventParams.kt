package com.braintreepayments.api

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams constructor(
    var payPalContextId: String? = null,
    var linkType: String? = null,
    var isVaultRequest: Boolean = false,
    var startTime: Long = -1,
    var endTime: Long = -1,
    var endpoint: String? = null
) {
  // TODO: this is a convenience constructor for Java; remove after Kotlin migration is complete
  constructor() : this(null, null, false, -1, -1, null)
}
