package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class UnionPayConfigurationUnitTest {
    @Test
    @Throws(JSONException::class)
    fun fromJson_parsesFullInput() {
        val input = JSONObject()
            .put("enabled", true)
        val (isEnabled) = UnionPayConfiguration(input)
        assertTrue(isEnabled)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val (isEnabled) = UnionPayConfiguration(null)
        assertFalse(isEnabled)
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val (isEnabled) = UnionPayConfiguration(JSONObject())
        assertFalse(isEnabled)
    }
}
