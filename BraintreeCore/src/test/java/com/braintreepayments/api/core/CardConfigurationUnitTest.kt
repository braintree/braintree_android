package com.braintreepayments.api.core

import org.robolectric.RobolectricTestRunner
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class CardConfigurationUnitTest {
    @Test
    fun `when input contains full card configuration data, all fields are parsed`() {
        val input = JSONObject()
            .put("collectDeviceData", true)
            .put(
                "supportedCardTypes", JSONArray()
                    .put("American Express")
                    .put("Discover")
                    .put("JCB")
                    .put("MasterCard")
                    .put("Visa")
            )
        val (supportedCardTypes, isFraudDataCollectionEnabled) = CardConfiguration(input)
        assertTrue(isFraudDataCollectionEnabled)
        assertEquals(5, supportedCardTypes.size)
        assertEquals("American Express", supportedCardTypes[0])
        assertEquals("Discover", supportedCardTypes[1])
        assertEquals("JCB", supportedCardTypes[2])
        assertEquals("MasterCard", supportedCardTypes[3])
        assertEquals("Visa", supportedCardTypes[4])
    }

    @Test
    fun `when input is null, CardConfiguration uses default values`() {
        val (supportedCardTypes, isFraudDataCollectionEnabled) = CardConfiguration(null)
        assertFalse(isFraudDataCollectionEnabled)
        assertEquals(0, supportedCardTypes.size)
    }

    @Test
    fun `when input is empty, CardConfiguration uses default values`() {
        val (supportedCardTypes, isFraudDataCollectionEnabled) = CardConfiguration(JSONObject())
        assertFalse(isFraudDataCollectionEnabled)
        assertEquals(0, supportedCardTypes.size)
    }
}
