package com.braintreepayments.api

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BraintreeOptions @JvmOverloads constructor(
    val context: Context,
    val sessionId: String? = null,
    val returnUrlScheme: String? = null,
    val initialAuthString: String? = null,
    val clientTokenProvider: ClientTokenProvider? = null,
    @IntegrationType.Integration val integrationType: String? = null,
)
