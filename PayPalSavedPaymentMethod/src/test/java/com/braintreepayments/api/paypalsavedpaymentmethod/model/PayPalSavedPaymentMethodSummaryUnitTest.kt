package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodSummaryUnitTest {

    @Test
    fun `fromJson parses an instrument response`() {
        val json = JSONObject(
            """
            {
              "payer": null,
              "paymentMethods": [
                {
                  "label": "CREDIT UNION 1",
                  "imageUrl": "https://example.com/bank.png",
                  "lastDigits": "3357",
                  "type": "BANK",
                  "subtype": null
                }
              ]
            }
            """.trimIndent()
        )

        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(json))

        assertEquals("CREDIT UNION 1", summary.paymentMethods.single().label)
        assertNull(summary.payer)
    }

    @Test
    fun `fromJson parses a display only payer response`() {
        val json = JSONObject(
            """{ "payer": { "email": "buyer@example.com", "editable": false }, "paymentMethods": [] }"""
        )

        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(json))
        val payer = requireNotNull(summary.payer)

        assertEquals(emptyList<PayPalSavedPaymentMethod>(), summary.paymentMethods)
        assertEquals("buyer@example.com", payer.email)
        assertFalse(payer.isEditable)
    }

    @Test
    fun `fromJson returns an empty summary when there is nothing to display`() {
        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(JSONObject()))

        assertEquals(emptyList<PayPalSavedPaymentMethod>(), summary.paymentMethods)
        assertNull(summary.payer)
    }

    @Test
    fun `fromJson drops malformed entries from the paymentMethods array`() {
        val json = JSONObject("""{ "paymentMethods": [ "not-an-object", { "type": "CARD", "label": "Visa" } ] }""")

        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(json))

        assertEquals("Visa", summary.paymentMethods.single().label)
    }

    @Test
    fun `fromJson drops empty instruments so the payer email fallback applies`() {
        val json = JSONObject("""{ "payer": { "email": "buyer@example.com" }, "paymentMethods": [ {} ] }""")

        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(json))

        assertEquals(emptyList<PayPalSavedPaymentMethod>(), summary.paymentMethods)
        assertEquals("buyer@example.com", requireNotNull(summary.payer).email)
    }

    @Test
    fun `fromJson drops a payer with a blank email so the component can hide`() {
        val json = JSONObject("""{ "payer": { "email": "" }, "paymentMethods": [] }""")

        val summary = requireNotNull(PayPalSavedPaymentMethodSummary.fromJson(json))

        assertEquals(emptyList<PayPalSavedPaymentMethod>(), summary.paymentMethods)
        assertNull(summary.payer)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalSavedPaymentMethodSummary.fromJson(null))
    }
}
