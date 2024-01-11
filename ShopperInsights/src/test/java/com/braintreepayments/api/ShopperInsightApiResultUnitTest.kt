package com.braintreepayments.api

import ShopperInsightApiResult
import org.junit.Assert.*
import org.junit.Test

class ShopperInsightApiResultUnitTest {

    @Test
    fun testVenmoFromJson() {
        // Sample JSON string
        val jsonString = """
            {
                "eligible_methods": {
                    "venmo": {
                        "can_be_vaulted": false,
                        "eligible_in_paypal_network": true,
                        "recommended": false
                    }
                }
            }
        """.trimIndent()

        // Convert JSON string to ShopperInsightApiResult object
        val result = ShopperInsightApiResult.fromJson(jsonString)

        // Assertions for Venmo
        val venmo = result.eligibleMethods.venmo
        assertNotNull(venmo)
        assertFalse(venmo!!.canBeVaulted)
        assertTrue(venmo.eligibleInPayPalNetwork)
        assertFalse(venmo.recommended)
        assertEquals(0, venmo.recommendedPriority)
    }

    @Test
    fun testPayPalFromJson() {
        // Sample JSON string
        val jsonString = """
            {
                "eligible_methods": {
                    "paypal": {
                        "can_be_vaulted": true,
                        "eligible_in_paypal_network": false,
                        "recommended": true,
                        "recommended_priority": 1
                    }
                }
            }
        """.trimIndent()

        // Convert JSON string to ShopperInsightApiResult object
        val result = ShopperInsightApiResult.fromJson(jsonString)

        // Assertions for PayPal
        val paypal = result.eligibleMethods.paypal
        assertNotNull(paypal)
        assertTrue(paypal!!.canBeVaulted)
        assertFalse(paypal.eligibleInPayPalNetwork)
        assertTrue(paypal.recommended)
        assertEquals(1, paypal.recommendedPriority)
    }
}
