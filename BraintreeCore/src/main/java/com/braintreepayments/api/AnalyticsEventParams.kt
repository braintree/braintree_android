package com.braintreepayments.api

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams constructor(
    var payPalContextId: String?,
    var linkType: String?,
    var isVaultRequest: Boolean,
) {
  constructor() : this(null, null, false)
}
