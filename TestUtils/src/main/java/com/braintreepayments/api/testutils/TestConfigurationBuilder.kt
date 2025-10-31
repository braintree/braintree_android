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

    fun clientApiUrl(clientApiUrl: String) = apply {
        put(clientApiUrl)
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

    fun visaCheckout(visaCheckoutConfigurationBuilder: TestVisaCheckoutConfigurationBuilder) = apply {
        try {
            put(JSONObject(visaCheckoutConfigurationBuilder.build()))
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

    fun paypal(): TestPayPalConfigurationBuilder {
        return try {
            TestPayPalConfigurationBuilder(jsonBody.getJSONObject("paypal"))
        } catch (ignored: JSONException) {
            TestPayPalConfigurationBuilder(true)
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

    companion object {
        fun <T> basicConfig(): T {
            return TestConfigurationBuilder().buildConfiguration()
        }
    }
}