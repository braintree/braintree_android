package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * An internal repository that holds properties set by the integrating merchant.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MerchantRepository {

    lateinit var applicationContext: Context
    lateinit var integrationType: IntegrationType

    /**
     * The authorization of the most recently created [BraintreeClient].
     *
     * Only read by components without a [BraintreeClient] reference (e.g. analytics);
     * clients must use [BraintreeClient.authorization] instead.
     */
    lateinit var authorization: Authorization
    lateinit var returnUrlScheme: String
    var appLinkReturnUri: Uri? = null

    var deepLinkFallbackUrlScheme: String? = null

    companion object {

        /**
         * Singleton instance of the MerchantRepository.
         */
        val instance: MerchantRepository by lazy { MerchantRepository() }
    }
}
