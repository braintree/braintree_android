package com.braintreepayments.api

internal data class AnalyticsEvent constructor(
    val name: String,
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val venmoInstalled: Boolean = false,
    val isVaultRequest: Boolean = false
)
