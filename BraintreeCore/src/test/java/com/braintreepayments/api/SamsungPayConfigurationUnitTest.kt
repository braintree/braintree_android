package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(RobolectricTestRunner::class)
class SamsungPayConfigurationUnitTest {

    @Test
    fun fromJson_parsesFullInput() {
        val input = JSONObject()
            .put("displayName", "sample display name")
            .put("serviceId", "sample-service-id")
            .put("samsungAuthorization", "sample-samsung-authorization")
            .put("environment", "SANDBOX")
            .put(
                "supportedCardBrands", JSONArray()
                    .put("american_express")
                    .put("diners")
                    .put("discover")
                    .put("jcb")
                    .put("maestro")
                    .put("mastercard")
                    .put("visa")
            )
        val sut = SamsungPayConfiguration(input)
        assertTrue(sut.isEnabled)
        assertEquals("sample display name", sut.merchantDisplayName)
        assertEquals("sample-service-id", sut.serviceId)
        assertEquals("sample-samsung-authorization", sut.samsungAuthorization)
        assertEquals("SANDBOX", sut.environment)
        val expected = Arrays.asList(
            "american_express", "diners", "discover", "jcb", "maestro", "mastercard", "visa"
        )
        assertEquals(expected, sut.supportedCardBrands)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val sut = SamsungPayConfiguration(null)
        assertFalse(sut.isEnabled)
        assertEquals("", sut.environment)
        assertEquals("", sut.merchantDisplayName)
        assertEquals("", sut.samsungAuthorization)
        assertEquals("", sut.serviceId)
        assertEquals(0, sut.supportedCardBrands.size.toLong())
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val sut = SamsungPayConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertEquals("", sut.environment)
        assertEquals("", sut.merchantDisplayName)
        assertEquals("", sut.samsungAuthorization)
        assertEquals("", sut.serviceId)
        assertEquals(0, sut.supportedCardBrands.size.toLong())
    }
}
