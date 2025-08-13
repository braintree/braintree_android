package com.braintreepayments.api.paypal

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditFinancingUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `creates PayPalCreditFinancing from JSON and returns null when empty`() {
        val payPalCreditFinancing = PayPalCreditFinancing.fromJson(null)

        assertNotNull(payPalCreditFinancing)
        assertFalse(payPalCreditFinancing.isCardAmountImmutable)
        assertEquals(0, payPalCreditFinancing.term)
        assertFalse(payPalCreditFinancing.hasPayerAcceptance)
        assertNull(payPalCreditFinancing.monthlyPayment)
        assertNull(payPalCreditFinancing.totalCost)
        assertNull(payPalCreditFinancing.totalInterest)
    }

    @Test
    @Throws(JSONException::class)
    fun `successfully creates PayPalCreditFinancing from standard JSON`() {
        val payPalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE
        val creditFinancingJsonObject = JSONObject(payPalAccountResponse).getJSONArray("paypalAccounts")
            .getJSONObject(0).getJSONObject("details")
            .getJSONObject("creditFinancingOffered")

        val payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancingJsonObject)

        assertFalse(payPalCreditFinancing.isCardAmountImmutable)
        assertEquals(18, payPalCreditFinancing.term)
        assertTrue(payPalCreditFinancing.hasPayerAcceptance)
        assertEquals("USD", payPalCreditFinancing.monthlyPayment?.currency)
        assertEquals("USD", payPalCreditFinancing.totalCost?.currency)
        assertEquals("USD", payPalCreditFinancing.totalInterest?.currency)
        assertEquals("13.88", payPalCreditFinancing.monthlyPayment?.value)
        assertEquals("250.00", payPalCreditFinancing.totalCost?.value)
        assertEquals("0.00", payPalCreditFinancing.totalInterest?.value)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates PayPalCreditFinancing from JSON without credit financing data`() {
        val payPalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_CREDIT_FINANCING_DATA
        val creditFinancingJsonObject = JSONObject(payPalAccountResponse).getJSONArray("paypalAccounts")
            .getJSONObject(0).getJSONObject("details")
            .getJSONObject("creditFinancingOffered")

        val payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancingJsonObject)

        assertFalse(payPalCreditFinancing.isCardAmountImmutable)
        assertEquals(18, payPalCreditFinancing.term)
        assertFalse(payPalCreditFinancing.hasPayerAcceptance)
        assertNull(payPalCreditFinancing.monthlyPayment?.currency)
        assertNull(payPalCreditFinancing.totalCost?.currency)
        assertNull(payPalCreditFinancing.totalInterest?.currency)
        assertNull(payPalCreditFinancing.monthlyPayment?.value)
        assertNull(payPalCreditFinancing.totalCost?.value)
        assertNull(payPalCreditFinancing.totalInterest?.value)
    }

    @Test
    @Throws(JSONException::class)
    fun `writes to Parcel and serializes correctly`() {
        val payPalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE
        val creditFinancingJsonObject = JSONObject(payPalAccountResponse).getJSONArray("paypalAccounts")
            .getJSONObject(0).getJSONObject("details")
            .getJSONObject("creditFinancingOffered")

        val preSerialized = PayPalCreditFinancing.fromJson(creditFinancingJsonObject)
        val parcel = Parcel.obtain().apply {
            preSerialized.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val payPalCreditFinancing = parcelableCreator<PayPalCreditFinancing>().createFromParcel(parcel)

        assertNotNull(payPalCreditFinancing)
        assertFalse(payPalCreditFinancing.isCardAmountImmutable)
        assertEquals(18, payPalCreditFinancing.term)
        assertTrue(payPalCreditFinancing.hasPayerAcceptance)
        assertEquals("USD", payPalCreditFinancing.monthlyPayment?.currency)
        assertEquals("USD", payPalCreditFinancing.totalCost?.currency)
        assertEquals("USD", payPalCreditFinancing.totalInterest?.currency)
        assertEquals("13.88", payPalCreditFinancing.monthlyPayment?.value)
        assertEquals("250.00", payPalCreditFinancing.totalCost?.value)
        assertEquals("0.00", payPalCreditFinancing.totalInterest?.value)
    }
}
