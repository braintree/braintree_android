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
        val sut = KountConfiguration(input)
        assertTrue(sut.isEnabled)
        assertEquals("123456", sut.kountMerchantId)
    }

    @Test
    fun fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        val sut = KountConfiguration(null)
        assertFalse(sut.isEnabled)
        assertEquals("", sut.kountMerchantId)
    }

    @Test
    fun fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        val sut = KountConfiguration(JSONObject())
        assertFalse(sut.isEnabled)
        assertEquals("", sut.kountMerchantId)
    }
}