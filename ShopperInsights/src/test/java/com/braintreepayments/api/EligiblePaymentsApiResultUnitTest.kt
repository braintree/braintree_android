package com.braintreepayments.api

import org.junit.Assert.*
import org.junit.Test

class EligiblePaymentsApiResultUnitTest {

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
        val result = EligiblePaymentsApiResult.fromJson(jsonString)

        // Assertions for PayPal
        val paypal = result.eligibleMethods.paypal
        assertNotNull(paypal)
        assertTrue(paypal!!.canBeVaulted)
        assertFalse(paypal.eligibleInPayPalNetwork)
        assertTrue(paypal.recommended)
        assertEquals(1, paypal.recommendedPriority)
    }
}
