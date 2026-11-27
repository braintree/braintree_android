package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodUnitTest {

    @Test
    fun `fromJson parses all fields`() {
        val json = JSONObject()
            .put("label", "PayPal Credit")
            .put("imageUrl", "https://example.com/paypal-credit.png")
            .put("lastDigits", "3357")
            .put("type", "PAYPAL_CREDIT")
            .put("subtype", "PAY_LATER_US")
            .put("id", "BA-123")
            .put("cardBrand", "VISA")

        val result = requireNotNull(PayPalSavedPaymentMethod.fromJson(json))

        assertEquals("PayPal Credit", result.label)
        assertEquals("https://example.com/paypal-credit.png", result.imageUrl)
        assertEquals("3357", result.lastDigits)
        assertEquals(PayPalSavedPaymentMethodType.PAYPAL_CREDIT, result.type)
        assertEquals("PAY_LATER_US", result.subtype)
        assertEquals("BA-123", result.id)
        assertEquals("VISA", result.cardBrand)
        assertEquals("PAYPAL_CREDIT", result.rawType)
    }

    @Test
    fun `fromJson sets type to null for an unrecognized type and keeps the rest of the instrument`() {
        val result = requireNotNull(
            PayPalSavedPaymentMethod.fromJson(JSONObject().put("type", "SOMETHING_NEW").put("label", "Visa"))
        )

        assertNull(result.type)
        assertEquals("SOMETHING_NEW", result.rawType)
        assertEquals("Visa", result.label)
    }

    @Test
    fun `fromJson returns null for missing and explicitly null optional fields`() {
        val json = JSONObject().put("type", "CARD").put("label", "Visa").put("subtype", JSONObject.NULL)

        val result = requireNotNull(PayPalSavedPaymentMethod.fromJson(json))

        assertNull(result.imageUrl)
        assertNull(result.lastDigits)
        assertNull(result.subtype)
        assertNull(result.id)
        assertNull(result.cardBrand)
    }

    @Test
    fun `fromJson returns null for an empty object`() {
        assertNull(PayPalSavedPaymentMethod.fromJson(JSONObject()))
    }

    @Test
    fun `fromJson returns null when label is absent`() {
        assertNull(PayPalSavedPaymentMethod.fromJson(JSONObject().put("type", "CARD")))
    }

    @Test
    fun `fromJson returns null when label is blank`() {
        assertNull(PayPalSavedPaymentMethod.fromJson(JSONObject().put("type", "CARD").put("label", " ")))
    }

    @Test
    fun `fromJson keeps an entry without a type, with a null type`() {
        val result = requireNotNull(PayPalSavedPaymentMethod.fromJson(JSONObject().put("label", "Visa")))

        assertNull(result.rawType)
        assertNull(result.type)
        assertEquals("Visa", result.label)
    }

    @Test
    fun `fromJson keeps an entry with a blank type, with a null type`() {
        val json = JSONObject().put("type", "").put("label", "Visa")

        val result = requireNotNull(PayPalSavedPaymentMethod.fromJson(json))

        assertEquals("", result.rawType)
        assertNull(result.type)
    }

    @Test
    fun `fromJson returns a null imageUrl for a javascript url`() {
        val json = JSONObject().put("type", "CARD").put("label", "Visa").put("imageUrl", "javascript:alert(1)")

        val result = requireNotNull(PayPalSavedPaymentMethod.fromJson(json))

        assertNull(result.imageUrl)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalSavedPaymentMethod.fromJson(null))
    }
}
