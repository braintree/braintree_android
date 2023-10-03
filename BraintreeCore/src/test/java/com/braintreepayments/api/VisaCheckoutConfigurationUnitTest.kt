package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutConfigurationUnitTest {

    @Test
    fun fromJson_parsesFullInput() {
        val input = JSONObject()
            .put("apikey", "sample-api-key")
            .put("externalClientId", "sample-external-client-id")
            .put(
                "supportedCardTypes", JSONArray()
                    .put("American Express")
                    .put("Visa")
                    .put("Discover")
                    .put("MasterCard")
            )
        val sut = VisaCheckoutConfiguration(input)
        assertTrue(sut.isEnabled)
        assertEquals("sample-api-key", sut.apiKey)
        assertEquals("sample-external-client-id", sut.externalClientId)
        val expectedCardBrands = Arrays.asList("AMEX", "VISA", "DISCOVER", "MASTERCARD")
        assertEquals(expectedCardBrands, sut.acceptedCardBrands)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val sut = VisaCheckoutConfiguration(null)
        assertFalse(sut.isEnabled)
        assertEquals("", sut.apiKey)
        assertEquals("", sut.externalClientId)
        assertTrue(sut.acceptedCardBrands.isEmpty())
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val sut = VisaCheckoutConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertEquals("", sut.apiKey)
        assertEquals("", sut.externalClientId)
        assertTrue(sut.acceptedCardBrands.isEmpty())
    }
}
