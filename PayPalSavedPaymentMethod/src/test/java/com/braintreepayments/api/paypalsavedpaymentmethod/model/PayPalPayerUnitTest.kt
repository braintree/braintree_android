package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalPayerUnitTest {

    @Test
    fun `fromJson parses email and editable`() {
        val result = requireNotNull(
            PayPalPayer.fromJson(JSONObject().put("email", "buyer@example.com").put("editable", true))
        )

        assertEquals("buyer@example.com", result.email)
        assertTrue(result.isEditable)
    }

    @Test
    fun `fromJson defaults isEditable to false when the field is absent`() {
        val result = requireNotNull(PayPalPayer.fromJson(JSONObject().put("email", "buyer@example.com")))

        assertFalse(result.isEditable)
    }

    @Test
    fun `fromJson defaults isEditable to false when the field is null`() {
        val result = requireNotNull(
            PayPalPayer.fromJson(JSONObject().put("email", "buyer@example.com").put("editable", JSONObject.NULL))
        )

        assertFalse(result.isEditable)
    }

    @Test
    fun `fromJson returns null when email is absent`() {
        assertNull(PayPalPayer.fromJson(JSONObject().put("editable", true)))
    }

    @Test
    fun `fromJson returns null when email is null`() {
        assertNull(PayPalPayer.fromJson(JSONObject().put("email", JSONObject.NULL)))
    }

    @Test
    fun `fromJson returns null when email is blank`() {
        assertNull(PayPalPayer.fromJson(JSONObject().put("email", "  ")))
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalPayer.fromJson(null))
    }
}
