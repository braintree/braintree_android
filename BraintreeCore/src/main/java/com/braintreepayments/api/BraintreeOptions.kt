package com.braintreepayments.api

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BraintreeOptions @JvmOverloads constructor(
    val context: Context,
    val authorization: Authorization,
    val sessionId: String? = null,
    val returnUrlScheme: String? = null,
    @IntegrationType.Integration val integrationType: String? = null,
)
