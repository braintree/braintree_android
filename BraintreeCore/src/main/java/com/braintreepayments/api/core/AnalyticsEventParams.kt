package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams @JvmOverloads constructor(
    var payPalContextId: String? = null,
    var linkType: String? = null,
    var isVaultRequest: Boolean = false,
    var startTime: Long? = null,
    var endTime: Long? = null,
    var endpoint: String? = null,
    val experiment: String? = null,
    val paymentMethodsDisplayed: List<String> = emptyList()
)
