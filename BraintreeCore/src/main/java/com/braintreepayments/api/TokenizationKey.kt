package com.braintreepayments.api


internal class TokenizationKey(tokenizationKey: String) : Authorization(tokenizationKey) {

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
        private const val DEVELOPMENT = "development"
        private const val SANDBOX = "sandbox"
        private const val PRODUCTION = "production"
        private const val CONFIG_V1 = "v1/configuration"

        @Throws(InvalidArgumentException::class)
        private fun getUrl(environment: String) = when (environment) {
            DEVELOPMENT -> BuildConfig.DEVELOPMENT_URL
            SANDBOX -> "https://api.sandbox.braintreegateway.com/"
            PRODUCTION -> "https://api.braintreegateway.com/"
            else -> throw InvalidArgumentException("Tokenization Key contained invalid environment")
        }
    }
}