package com.braintreepayments.api.venmo

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoAccountNonceUnitTest {

    private var NONCE = "venmo-nonce"
    private var USERNAME = "venmo-username"
    private var VENMO_NONCE = VenmoAccountNonce(
        string = NONCE,
        isDefault = false,
        email = null,
        externalId = null,
        firstName = null,
        lastName = null,
        phoneNumber = null,
        username = USERNAME,
        billingAddress = null,
        shippingAddress = null
    )

    @Test
    @Throws(JSONException::class)
    fun `creates VenmoAccountNonce from JSON and parses response correctly`() {
        val venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)
        )

        assertEquals("venmojoe", venmoAccountNonce.username)
        assertEquals("fake-venmo-nonce", venmoAccountNonce.string)
        assertTrue(venmoAccountNonce.isDefault)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VenmoAccountNonce with paymentMethodId from JSON and parses response correctly`() {
        val venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON)
        )

        assertEquals("@sampleuser", venmoAccountNonce.username)
        assertEquals("sample-payment-method-id", venmoAccountNonce.string)
        assertEquals("venmo-email", venmoAccountNonce.email)
        assertEquals("venmo-external-id", venmoAccountNonce.externalId)
        assertEquals("venmo-first-name", venmoAccountNonce.firstName)
        assertEquals("venmo-last-name", venmoAccountNonce.lastName)
        assertEquals("venmo-phone-number", venmoAccountNonce.phoneNumber)
        assertFalse(venmoAccountNonce.isDefault)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VenmoAccountNonce with shipping and billing addresses from JSON and parses response correctly`() {
        val venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON_WITH_ADDRESSES)
        )

        assertEquals("123 Fake St.", venmoAccountNonce.billingAddress?.streetAddress)
        assertEquals("Apt. 3", venmoAccountNonce.billingAddress?.extendedAddress)
        assertEquals("Oakland", venmoAccountNonce.billingAddress?.locality)
        assertEquals("CA", venmoAccountNonce.billingAddress?.region)
        assertEquals("94602", venmoAccountNonce.billingAddress?.postalCode)
        assertEquals("US", venmoAccountNonce.billingAddress?.countryCodeAlpha2)

        assertEquals("789 Fake St.", venmoAccountNonce.shippingAddress?.streetAddress)
        assertEquals("Apt. 2", venmoAccountNonce.shippingAddress?.extendedAddress)
        assertEquals("Dallas", venmoAccountNonce.shippingAddress?.locality)
        assertEquals("TX", venmoAccountNonce.shippingAddress?.region)
        assertEquals("75001", venmoAccountNonce.shippingAddress?.postalCode)
        assertEquals("US", venmoAccountNonce.shippingAddress?.countryCodeAlpha2)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VenmoAccountNonce with paymentMethodId and null payerInfo from JSON and parses response correctly`() {
        val venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON)
        )

        assertEquals("@sampleuser", venmoAccountNonce.username)
        assertEquals("sample-payment-method-id", venmoAccountNonce.string)
        assertNull(venmoAccountNonce.email)
        assertNull(venmoAccountNonce.externalId)
        assertNull(venmoAccountNonce.firstName)
        assertNull(venmoAccountNonce.lastName)
        assertNull(venmoAccountNonce.phoneNumber)
        assertFalse(venmoAccountNonce.isDefault)
    }

    @Test
    fun `gets Nonce and returns Nonce correctly`() {
        assertEquals(NONCE, VENMO_NONCE.string)
    }

    @Test
    fun `gets username and returns username correctly`() {
        assertEquals(USERNAME, VENMO_NONCE.username)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VenmoAccountNonce from JSON and parcels it correctly`() {
        val venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON)
        )

        val parcel = Parcel.obtain().apply {
            venmoAccountNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<VenmoAccountNonce>().createFromParcel(parcel)

        assertEquals("@sampleuser", parceled.username)
        assertEquals("sample-payment-method-id", parceled.string)
        assertEquals("venmo-email", parceled.email)
        assertEquals("venmo-external-id", parceled.externalId)
        assertEquals("venmo-first-name", parceled.firstName)
        assertEquals("venmo-last-name", parceled.lastName)
        assertEquals("venmo-phone-number", parceled.phoneNumber)
    }
}
