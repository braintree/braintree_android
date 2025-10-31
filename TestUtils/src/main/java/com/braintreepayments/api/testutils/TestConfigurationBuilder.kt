package com.braintreepayments.api.testutils

import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.GraphQLConstants.Features
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class TestConfigurationBuilder : JSONBuilder {

    constructor() : super() {
        clientApiUrl("client_api_url")
        environment("test")
        merchantId("integration_merchant_id")
    }

    constructor(json: JSONObject) : super(json)

    fun assetsUrl(assetsUrl: String) = apply {
        put(assetsUrl)
    }

    fun clientApiUrl(clientApiUrl: String) = apply {
        put(clientApiUrl)
    }

    fun challenges(vararg challenges: String) = apply {
        val challengesJson = JSONArray()
        challenges.forEach { challengesJson.put(it) }
        put(challengesJson)
    }

    fun environment(environment: String) = apply {
        put(environment)
    }

    fun merchantId(merchantId: String) = apply {
        put(merchantId)
    }

    fun merchantAccountId(merchantAccountId: String) = apply {
        put(merchantAccountId)
    }

    fun threeDSecureEnabled(threeDSecureEnabled: Boolean) = apply {
        put(threeDSecureEnabled.toString())
    }

    fun cardinalAuthenticationJWT(jwt: String) = apply {
        put(jwt)
    }

    fun withAnalytics() = apply {
        analytics("http://example.com")
    }

    fun analytics(analyticsUrl: String) = apply {
        try {
            val analyticsJson = JSONObject().apply {
                put("url", analyticsUrl)
            }
            put(analyticsJson)
        } catch (ignored: JSONException) {
        }
    }

    fun paypal(builder: TestPayPalConfigurationBuilder) = apply {
        try {
            paypalEnabled(true)
            put(JSONObject(builder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun paypalEnabled(enabled: Boolean) = apply {
        put(enabled)
        if (enabled) {
            try {
                put("paypal", JSONObject(TestPayPalConfigurationBuilder(true).build()))
            } catch (ignored: JSONException) {
            }
        }
    }

    fun googlePay(builder: TestGooglePayConfigurationBuilder) = apply {
        try {
            put("androidPay", JSONObject(builder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun payWithVenmo(venmoConfigurationBuilder: TestVenmoConfigurationBuilder) = apply {
        try {
            put(JSONObject(venmoConfigurationBuilder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun visaCheckout(visaCheckoutConfigurationBuilder: TestVisaCheckoutConfigurationBuilder) = apply {
        try {
            put(JSONObject(visaCheckoutConfigurationBuilder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun braintreeApi(braintreeApiConfigurationBuilder: TestBraintreeApiConfigurationBuilder) = apply {
        try {
            put(JSONObject(braintreeApiConfigurationBuilder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun graphQL() = apply {
        try {
            val graphQLJson = JSONObject().apply {
                put("url", "http://10.0.2.2:8080/graphql")
                put("features", JSONArray().put(Features.TOKENIZE_CREDIT_CARDS))
            }
            put(graphQLJson)
        } catch (ignored: JSONException) {
        }
    }

    fun graphQL(graphQLConfigurationBuilder: TestGraphQLConfigurationBuilder) = apply {
        try {
            put(JSONObject(graphQLConfigurationBuilder.build()))
        } catch (ignored: JSONException) {
        }
    }

    fun <T> buildConfiguration(): T {
        return try {
            Configuration.fromJson(build()) as T
        } catch (ignored: Exception) {
            build() as T
        }
    }

    fun payWithVenmo(): TestVenmoConfigurationBuilder {
        return try {
            TestVenmoConfigurationBuilder(jsonBody.getJSONObject("payWithVenmo"))
        } catch (ignored: JSONException) {
            TestVenmoConfigurationBuilder()
        }
    }

    fun googlePay(): TestGooglePayConfigurationBuilder {
        return try {
            TestGooglePayConfigurationBuilder(jsonBody.getJSONObject("androidPay"))
        } catch (ignored: JSONException) {
            TestGooglePayConfigurationBuilder()
        }
    }

    fun paypal(): TestPayPalConfigurationBuilder {
        return try {
            TestPayPalConfigurationBuilder(jsonBody.getJSONObject("paypal"))
        } catch (ignored: JSONException) {
            TestPayPalConfigurationBuilder(true)
        }
    }

    fun visaCheckout(): TestVisaCheckoutConfigurationBuilder {
        return try {
            TestVisaCheckoutConfigurationBuilder(jsonBody.getJSONObject("visaCheckout"))
        } catch (ignored: JSONException) {
            TestVisaCheckoutConfigurationBuilder()
        }
    }

    fun graphQLConfigurationBuilder(): TestGraphQLConfigurationBuilder {
        return try {
            TestGraphQLConfigurationBuilder(jsonBody.getJSONObject("graphQL"))
        } catch (ignored: JSONException) {
            TestGraphQLConfigurationBuilder()
        }
    }

    class TestVenmoConfigurationBuilder : JSONBuilder {

        constructor() : super()
        constructor(json: JSONObject) : super(json)

        fun accessToken(accessToken: String) = apply {
            put(accessToken)
        }

        fun merchantId(merchantId: String) = apply {
            put(merchantId)
        }

        fun environment(environment: String) = apply {
            put(environment)
        }
    }

    class TestPayPalConfigurationBuilder : JSONBuilder {

        constructor(enabled: Boolean) : super() {
            if (enabled) {
                environment("test")
                displayName("displayName")
                clientId("clientId")
                privacyUrl("http://privacy.gov")
                userAgreementUrl("http://i.agree.biz")
            }
        }

        constructor(json: JSONObject) : super(json)

        fun displayName(displayName: String) = apply {
            put(displayName)
        }

        fun clientId(clientId: String) = apply {
            put(clientId)
        }

        fun privacyUrl(privacyUrl: String) = apply {
            put(privacyUrl)
        }

        fun userAgreementUrl(userAgreementUrl: String) = apply {
            put(userAgreementUrl)
        }

        fun directBaseUrl(directBaseUrl: String) = apply {
            put(directBaseUrl)
        }

        fun environment(environment: String) = apply {
            put(environment)
        }

        fun touchDisabled(touchDisabled: Boolean) = apply {
            put(touchDisabled.toString())
        }

        fun currencyIsoCode(currencyIsoCode: String) = apply {
            put(currencyIsoCode)
        }

        fun billingAgreementsEnabled(billingAgreementsEnabled: Boolean) = apply {
            put(billingAgreementsEnabled.toString())
        }
    }

    class TestGooglePayConfigurationBuilder : JSONBuilder {

        constructor() : super()
        constructor(json: JSONObject) : super(json)

        fun enabled(enabled: Boolean) = apply {
            put(enabled.toString())
        }

        fun googleAuthorizationFingerprint(fingerprint: String) = apply {
            put(fingerprint)
        }

        fun environment(environment: String) = apply {
            put(environment)
        }

        fun displayName(displayName: String) = apply {
            put(displayName)
        }

        fun supportedNetworks(supportedNetworks: Array<String>) = apply {
            put(JSONArray(supportedNetworks.toList()))
        }

        fun paypalClientId(paypalClientId: String) = apply {
            put(paypalClientId)
        }
    }

    class TestVisaCheckoutConfigurationBuilder : JSONBuilder {

        constructor() : super()
        constructor(json: JSONObject) : super(json)

        fun apikey(apikey: String) = apply {
            put(apikey)
        }

        fun externalClientId(externalClientId: String) = apply {
            put(externalClientId)
        }

        fun supportedCardTypes(vararg supportedCardTypes: String) = apply {
            put(JSONArray(supportedCardTypes.toList()))
        }
    }

    class TestBraintreeApiConfigurationBuilder : JSONBuilder {

        constructor() : super()
        constructor(json: JSONObject) : super(json)

        fun accessToken(accessToken: String) = apply {
            put(accessToken)
        }

        fun url(url: String) = apply {
            put(url)
        }
    }

    class TestGraphQLConfigurationBuilder : JSONBuilder {

        constructor() : super()
        constructor(json: JSONObject) : super(json)

        fun url(url: String) = apply {
            put(url)
        }

        fun features(vararg features: String) = apply {
            val jsonFeatures = JSONArray()
            features.forEach { jsonFeatures.put(it) }
            put(jsonFeatures)
        }
    }

    companion object {
        @JvmStatic
        fun <T> basicConfig(): T {
            return TestConfigurationBuilder().buildConfiguration()
        }
    }
}