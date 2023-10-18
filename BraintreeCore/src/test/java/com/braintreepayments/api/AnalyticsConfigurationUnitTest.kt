package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AnalyticsConfigurationUnitTest {
    @Test
    fun fromJson_parsesFullInput() {
        val input = JSONObject()
            .put("url", "https://example.com/analytics")
        val sut = AnalyticsConfiguration(input)
        assertTrue(sut.isEnabled)
        assertEquals("https://example.com/analytics", sut.url)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val sut = AnalyticsConfiguration(null)
        assertFalse(sut.isEnabled)
        assertTrue(sut.url == "")
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val sut = AnalyticsConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertTrue(sut.url == "")
    }
}
