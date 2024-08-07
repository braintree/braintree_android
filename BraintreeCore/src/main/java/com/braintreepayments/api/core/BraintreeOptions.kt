package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri

internal data class BraintreeOptions(
    val context: Context,
    val authorization: Authorization,
    val sessionId: String? = null,
    val returnUrlScheme: String? = null,
    val appLinkReturnUri: Uri? = null,
    val integrationType: IntegrationType? = null,
)
