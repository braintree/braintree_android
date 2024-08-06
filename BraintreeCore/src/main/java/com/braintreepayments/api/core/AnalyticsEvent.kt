package com.braintreepayments.api.core

internal data class AnalyticsEvent(
    val name: String,
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val venmoInstalled: Boolean = false,
    val isVaultRequest: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
