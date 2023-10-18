package com.braintreepayments.api

import com.braintreepayments.api.Authorization.Companion.fromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.robolectric.RobolectricTestRunner

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class TokenizationKeyUnitTest {

    companion object {
        private const val TOKENIZATION_KEY = "development_testing_integration_merchant_id"
    }

    @Test
    fun fromString_acceptsATokenizationKey() {
        val tokenizationKey = fromString(TOKENIZATION_KEY)
        assertEquals(TOKENIZATION_KEY, tokenizationKey.bearer)
    }

    @Test
    fun fromString_returnsInvalidTokenForNonTokenizationKeys() {
        val result = fromString("{}")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun fromString_parsesEnvironment() {
        val tokenizationKey = fromString(TOKENIZATION_KEY) as TokenizationKey
        assertEquals("development", tokenizationKey.environment)
    }

    @Test
    fun fromString_parsesMerchantId() {
        val tokenizationKey = fromString(TOKENIZATION_KEY) as TokenizationKey
        assertEquals("integration_merchant_id", tokenizationKey.merchantId)
    }

    @Test
    fun fromString_setsUrlForDevelopment() {
        val tokenizationKey = fromString(TOKENIZATION_KEY) as TokenizationKey
        assertEquals(
            BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/",
            tokenizationKey.url
        )
        assertEquals(
            BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/v1/configuration",
            tokenizationKey.configUrl
        )
    }

    @Test
    fun fromString_setsUrlForSandbox() {
        val tokenizationKey = fromString(
            "sandbox_fjajdkd_integration_merchant_id"
        ) as TokenizationKey
        assertEquals(
            "https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/",
            tokenizationKey.url
        )
        assertEquals(
            "https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration",
            tokenizationKey.configUrl
        )
    }

    @Test
    fun fromString_setsUrlForProduction() {
        val tokenizationKey = fromString(
            "production_fjajdkd_integration_merchant_id"
        ) as TokenizationKey
        assertEquals(
            "https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/",
            tokenizationKey.url
        )
        assertEquals(
            "https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration",
            tokenizationKey.configUrl
        )
    }

    @Test
    fun fromString_returnsInvalidTokenForInvalidEnvironments() {
        val result = fromString("test_fjajdkd_integration_merchant_id")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun getBearer_returnsTokenizationKey() {
        assertEquals(TOKENIZATION_KEY, fromString(TOKENIZATION_KEY).bearer)
    }
}
