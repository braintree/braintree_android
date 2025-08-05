package com.braintreepayments.api.paypal

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalAccountNonceUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses PayPalAccountNonce with credit financing offer`() {
        val payPalAccountNonce = PayPalAccountNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        )

        assertNotNull(payPalAccountNonce)
        assertEquals("fake-authenticate-url", payPalAccountNonce.authenticateUrl)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.string)
        assertEquals("paypalaccount@example.com", payPalAccountNonce.email)
        assertEquals("123 Fake St.", payPalAccountNonce.billingAddress.streetAddress)
        assertEquals("Apt. 3", payPalAccountNonce.billingAddress.extendedAddress)
        assertEquals("Oakland", payPalAccountNonce.billingAddress.locality)
        assertEquals("CA", payPalAccountNonce.billingAddress.region)
        assertEquals("94602", payPalAccountNonce.billingAddress.postalCode)
        assertEquals("US", payPalAccountNonce.billingAddress.countryCodeAlpha2)
        payPalAccountNonce.creditFinancing?.let {
            assertFalse(it.isCardAmountImmutable)
            assertEquals("USD", it.monthlyPayment?.currency)
            assertEquals("13.88", it.monthlyPayment?.value)
            assertTrue(it.hasPayerAcceptance)
            assertEquals(18, it.term)
            assertEquals("USD", it.totalCost?.currency)
            assertEquals("250.00", it.totalCost?.value)
            assertEquals("USD", it.totalInterest?.currency)
            assertEquals("0.00", it.totalInterest?.value)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses PayPalAccountNonce without credit financing offer`() {
        val response = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").apply {
            remove("creditFinancingOffered")
        }
        val payPalAccountNonce = PayPalAccountNonce.fromJSON(response)
        assertNotNull(payPalAccountNonce)
        assertEquals("fake-authenticate-url", payPalAccountNonce.authenticateUrl)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.string)
        assertEquals("paypalaccount@example.com", payPalAccountNonce.email)
        assertEquals("123 Fake St.", payPalAccountNonce.billingAddress.streetAddress)
        assertEquals("Apt. 3", payPalAccountNonce.billingAddress.extendedAddress)
        assertEquals("Oakland", payPalAccountNonce.billingAddress.locality)
        assertEquals("CA", payPalAccountNonce.billingAddress.region)
        assertEquals("94602", payPalAccountNonce.billingAddress.postalCode)
        assertEquals("US", payPalAccountNonce.billingAddress.countryCodeAlpha2)

        assertNull(payPalAccountNonce.creditFinancing)
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses PayPalAccountNonce without address and returns empty shipping and billing address`() {
        val response = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_ADDRESSES)
        val payPalAccountNonce = PayPalAccountNonce.fromJSON(response)

        assertNotNull(payPalAccountNonce.shippingAddress)
        assertNotNull(payPalAccountNonce.billingAddress)
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses PayPalAccountNonce and parcels it correctly`() {
        val payPalAccountNonce = PayPalAccountNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        )
        val parcel = Parcel.obtain().apply {
            payPalAccountNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<PayPalAccountNonce>().createFromParcel(parcel)

        assertEquals("fake-authenticate-url", parceled.authenticateUrl)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.string)
        assertEquals("paypalaccount@example.com", parceled.email)
        assertEquals("123 Fake St.", parceled.billingAddress.streetAddress)
        assertEquals("Apt. 3", parceled.billingAddress.extendedAddress)
        assertEquals("Oakland", parceled.billingAddress.locality)
        assertEquals("CA", parceled.billingAddress.region)
        assertEquals("94602", parceled.billingAddress.postalCode)
        assertEquals("US", parceled.billingAddress.countryCodeAlpha2)
        parceled.creditFinancing?.let {
            assertFalse(it.isCardAmountImmutable)
            assertEquals("USD", it.monthlyPayment?.currency)
            assertEquals("13.88", it.monthlyPayment?.value)
            assertTrue(it.hasPayerAcceptance)
            assertEquals(18, it.term)
            assertEquals("USD", it.totalCost?.currency)
            assertEquals("250.00", it.totalCost?.value)
            assertEquals("USD", it.totalInterest?.currency)
            assertEquals("0.00", it.totalInterest?.value)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses PayPalAccountNonce without credit financing and parcels it correctly`() {
        val response = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").apply {
            remove("creditFinancingOffered")
        }
        val payPalAccountNonce = PayPalAccountNonce.fromJSON(response)
        assertNull(payPalAccountNonce.creditFinancing)

        val parcel = Parcel.obtain().apply {
            payPalAccountNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<PayPalAccountNonce>().createFromParcel(parcel)

        assertNull(parceled.creditFinancing)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.string)
        assertEquals("paypalaccount@example.com", parceled.email)
        assertEquals("123 Fake St.", parceled.billingAddress.streetAddress)
        assertEquals("Apt. 3", parceled.billingAddress.extendedAddress)
        assertEquals("Oakland", parceled.billingAddress.locality)
        assertEquals("CA", parceled.billingAddress.region)
        assertEquals("94602", parceled.billingAddress.postalCode)
        assertEquals("US", parceled.billingAddress.countryCodeAlpha2)
    }
}
