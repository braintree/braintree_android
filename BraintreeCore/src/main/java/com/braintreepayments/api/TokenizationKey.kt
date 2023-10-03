package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TokenizationKey(tokenizationKey: String) : Authorization(tokenizationKey) {

    override val configUrl: String
    override val bearer: String = toString()

    val environment: String
    val merchantId: String
    val url: String

    init {
        val tokenizationKeyParts = tokenizationKey.split("_", limit = 3)
        environment = tokenizationKeyParts[0]
        merchantId = tokenizationKeyParts[2]
        url = getUrl(environment) + "merchants/" +
                merchantId + "/client_api/"
        configUrl = url + CONFIG_V1
    }

    companion object {
        const val MATCHER = "^[a-zA-Z0-9]+_[a-zA-Z0-9]+_[a-zA-Z0-9_]+$"
        private const val DEVELOPMENT_URL = BuildConfig.DEVELOPMENT_URL
        private const val SANDBOX_URL = "https://api.sandbox.braintreegateway.com/"
        private const val PRODUCTION_URL = "https://api.braintreegateway.com/"
        private const val CONFIG_V1 = "v1/configuration"

        @Throws(InvalidArgumentException::class)
        private fun getUrl(environment: String) = when (environment) {
            "development" -> DEVELOPMENT_URL
            "sandbox" -> SANDBOX_URL
            "production" -> PRODUCTION_URL
            else -> throw InvalidArgumentException("Tokenization Key contained invalid environment")
        }
    }
}
