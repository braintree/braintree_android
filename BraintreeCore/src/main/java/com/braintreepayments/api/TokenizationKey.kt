package com.braintreepayments.api


internal class TokenizationKey(tokenizationKey: String) : Authorization(tokenizationKey) {

    val environment: String
    val merchantId: String
    val url: String

    init {
        val tokenizationKeyParts = tokenizationKey.split("_", limit = 3)
        environment = tokenizationKeyParts[0]
        merchantId = tokenizationKeyParts[2]
        url = BraintreeEnvironment.getUrl(environment) + "merchants/" +
                merchantId + "/client_api/"
    }

    companion object {
        const val MATCHER = "^[a-zA-Z0-9]+_[a-zA-Z0-9]+_[a-zA-Z0-9_]+$"
        private const val CONFIG_V1 = "v1/configuration"
    }

    public override fun getConfigUrl(): String {
        return url + CONFIG_V1
    }

    public override fun getBearer(): String {
        return toString()
    }

    private enum class BraintreeEnvironment(
        private val mEnvironment: String,
        private val mUrl: String
    ) {
        DEVELOPMENT("development", BuildConfig.DEVELOPMENT_URL),
        SANDBOX(
            "sandbox",
            "https://api.sandbox.braintreegateway.com/"
        ),
        PRODUCTION("production", "https://api.braintreegateway.com/");

        companion object {
            @Throws(InvalidArgumentException::class)
            fun getUrl(environment: String): String {
                for (braintreeEnvironment in values()) {
                    if (braintreeEnvironment.mEnvironment == environment) {
                        return braintreeEnvironment.mUrl
                    }
                }
                throw InvalidArgumentException("Tokenization Key contained invalid environment")
            }
        }
    }

}