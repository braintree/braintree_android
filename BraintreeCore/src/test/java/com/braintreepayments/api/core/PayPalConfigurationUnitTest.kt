package com.braintreepayments.api.core

import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PayPalConfigurationUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `when json contains all fields, PayPalConfiguration parses all values`() {
        val input = JSONObject()
            .put("displayName", "sample display name")
            .put("clientId", "sample-client-id")
            .put("privacyUrl", "https://example.com/privacy_url")
            .put("userAgreementUrl", "https://example.com/user_agreement_url")
            .put("directBaseUrl", "https://example.com/direct_base_url")
            .put("environment", "live")
            .put("touchDisabled", true)
            .put("currencyIsoCode", "USD")

        val sut = PayPalConfiguration(input)
        Assert.assertEquals("sample display name", sut.displayName)
        Assert.assertEquals("sample-client-id", sut.clientId)
        Assert.assertEquals("https://example.com/privacy_url", sut.privacyUrl)
        Assert.assertEquals("https://example.com/user_agreement_url", sut.userAgreementUrl)
        Assert.assertEquals("https://example.com/direct_base_url/v1/", sut.directBaseUrl)
        Assert.assertEquals("live", sut.environment)
        Assert.assertEquals("USD", sut.currencyIsoCode)
        assertTrue(sut.isTouchDisabled)
    }

    @Test
    fun `when input json is null, PayPalConfiguration has null fields and touch disabled is true`() {
        val sut = PayPalConfiguration(null)
        assertNull(sut.displayName)
        assertNull(sut.clientId)
        assertNull(sut.privacyUrl)
        assertNull(sut.userAgreementUrl)
        assertNull(sut.directBaseUrl)
        assertNull(sut.environment)
        assertTrue(sut.isTouchDisabled)
    }

    @Test
    fun `when input json is empty, PayPalConfiguration has null fields and touch disabled is true`() {
        val sut = PayPalConfiguration(JSONObject())
        assertNull(sut.displayName)
        assertNull(sut.clientId)
        assertNull(sut.privacyUrl)
        assertNull(sut.userAgreementUrl)
        assertNull(sut.directBaseUrl)
        assertNull(sut.environment)
        assertTrue(sut.isTouchDisabled)
    }
}
