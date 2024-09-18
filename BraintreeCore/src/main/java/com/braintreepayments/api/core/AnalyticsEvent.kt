package com.braintreepayments.api.core

internal data class AnalyticsEvent(
    val name: String,
    val timestamp: Long,
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val isVaultRequest: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null
)
