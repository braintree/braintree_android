package com.braintreepayments.api.core

import com.braintreepayments.api.core.Authorization.Companion.fromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenizationKeyUnitTest {

    companion object {
        private const val TOKENIZATION_KEY = "development_testing_integration_merchant_id"
    }

    @Test
    fun `fromString parses a valid tokenization key`() {
        val tokenizationKey = fromString(TOKENIZATION_KEY)
        assertEquals(TOKENIZATION_KEY, tokenizationKey.bearer)
    }

    @Test
    fun `when input is not a tokenization key, fromString returns InvalidAuthorization`() {
        val result = fromString("{}")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun `fromString parses environment from tokenization key`() {
        val tokenizationKey = fromString(TOKENIZATION_KEY) as TokenizationKey
        assertEquals("development", tokenizationKey.environment)
    }

    @Test
    fun `fromString parses merchantId from tokenization key`() {
        val tokenizationKey = fromString(TOKENIZATION_KEY) as TokenizationKey
        assertEquals("integration_merchant_id", tokenizationKey.merchantId)
    }

    @Test
    fun `when environment is development, fromString sets url and configUrl to development endpoints`() {
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
    fun `when environment is sandbox, fromString sets url and configUrl to sandbox endpoints`() {
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
    fun `when environment is production, fromString sets url and configUrl to production endpoints`() {
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
    fun `when environment is invalid, fromString returns InvalidAuthorization`() {
        val result = fromString("test_fjajdkd_integration_merchant_id")
        assertTrue(result is InvalidAuthorization)
    }

    @Test
    fun `bearer returns the original tokenization key string`() {
        assertEquals(TOKENIZATION_KEY, fromString(TOKENIZATION_KEY).bearer)
    }
}
