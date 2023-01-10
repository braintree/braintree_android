package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class KountConfigurationUnitTest {
    
    @Test
    fun fromJson_parsesFullInput() {
        val input = JSONObject()
            .put("kountMerchantId", "123456")
        val (kountMerchantId, isEnabled) = KountConfiguration(input)
        assertTrue(isEnabled)
        assertEquals("123456", kountMerchantId)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val (kountMerchantId, isEnabled) = KountConfiguration(null)
        assertFalse(isEnabled)
        assertEquals("", kountMerchantId)
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val (kountMerchantId, isEnabled) = KountConfiguration(JSONObject())
        assertFalse(isEnabled)
        assertEquals("", kountMerchantId)
    }
}