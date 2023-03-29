package com.braintreepayments.api

import android.content.Context

internal data class BraintreeOptions(
    val context: Context,
    val sessionId: String? = null,
    val returnUrlScheme: String? = null,
    val initialAuthString: String? = null,
    val clientTokenProvider: ClientTokenProvider? = null,
    @IntegrationType.Integration val integrationType: String? = null,
)
