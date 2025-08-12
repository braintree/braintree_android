package com.braintreepayments.api.paypal

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditFinancingAmountUnitTest {

    @Test
    fun `from JSON returns empty object if null`() {
        val creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(null)

        assertNotNull(creditFinancingAmount)
        assertNull(creditFinancingAmount.currency)
        assertNull(creditFinancingAmount.value)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates CreditFinancingAmount from standard JSON`() {
        val json = "{\"currency\": \"USD\", \"value\": \"123.45\"}"
        val creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(JSONObject(json))

        assertEquals("USD", creditFinancingAmount.currency)
        assertEquals("123.45", creditFinancingAmount.value)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates CreditFinancingAmount from JSON with missing currency`() {
        val json = "{\"value\": \"123.45\"}"
        val creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(JSONObject(json))

        assertNull(creditFinancingAmount.currency)
        assertEquals("123.45", creditFinancingAmount.value)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates CreditFinancingAmount from JSON with missing value`() {
        val json = "{\"currency\": \"USD\"}"
        val creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(JSONObject(json))

        assertNull(creditFinancingAmount.value)
        assertEquals("USD", creditFinancingAmount.currency)
    }

    @Test
    @Throws(JSONException::class)
    fun `write to Parcel serializes correctly`() {
        val json = "{\"currency\": \"USD\", \"value\": \"123.45\"}"
        val preSerialized = PayPalCreditFinancingAmount.fromJson(JSONObject(json))

        val parcel = Parcel.obtain().apply {
            preSerialized.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val creditFinancingAmount = parcelableCreator<PayPalCreditFinancingAmount>().createFromParcel(parcel)

        assertNotNull(creditFinancingAmount)
        assertEquals("USD", creditFinancingAmount.currency)
        assertEquals("123.45", creditFinancingAmount.value)
    }
}
