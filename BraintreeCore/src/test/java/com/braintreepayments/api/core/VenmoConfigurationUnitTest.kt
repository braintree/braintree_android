package com.braintreepayments.api.core

import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class VenmoConfigurationUnitTest {
    @Test
    @Throws(JSONException::class)
    fun `when json contains all fields, VenmoConfiguration parses all values and isAccessTokenValid is true`() {
        val input = JSONObject()
            .put("accessToken", "sample-access-token")
            .put("environment", "sample-environment")
            .put("merchantId", "sample-merchant-id")
        val sut = VenmoConfiguration(input)
        assertEquals("sample-access-token", sut.accessToken)
        assertEquals("sample-environment", sut.environment)
        assertEquals("sample-merchant-id", sut.merchantId)
        assertTrue(sut.isAccessTokenValid)
    }

    @Test
    fun `when input json is null, VenmoConfiguration has empty fields and isAccessTokenValid is false`() {
        val sut = VenmoConfiguration(null)
        assertEquals("", sut.accessToken)
        assertEquals("", sut.environment)
        assertEquals("", sut.merchantId)
        assertFalse(sut.isAccessTokenValid)
    }

    @Test
    fun `when input json is empty, VenmoConfiguration has empty fields and isAccessTokenValid is false`() {
        val sut = VenmoConfiguration(JSONObject())
        assertEquals("", sut.accessToken)
        assertEquals("", sut.environment)
        assertEquals("", sut.merchantId)
        assertFalse(sut.isAccessTokenValid)
    }
}
