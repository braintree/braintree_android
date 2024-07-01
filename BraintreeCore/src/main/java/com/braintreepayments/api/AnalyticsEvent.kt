package com.braintreepayments.api

import androidx.annotation.RestrictTo

internal data class AnalyticsEvent constructor(
    val name: String,
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val venmoInstalled: Boolean = false,
    val isVaultRequest: Boolean = false
)
