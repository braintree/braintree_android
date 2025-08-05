package com.braintreepayments.api.localpayment

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentNonceUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `fromJSON parses LocalPaymentNonce correctly`() {
        val response = LocalPaymentNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE))

        assertNotNull(response)
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", response.string)
        assertEquals("jon@getbraintree.com", response.email)
        assertEquals("836486 of 22321 Park Lake", response.shippingAddress.streetAddress)
        assertEquals("Apt B", response.shippingAddress.extendedAddress)
        assertEquals("Den Haag", response.shippingAddress.locality)
        assertEquals("CA", response.shippingAddress.region)
        assertEquals("2585 GJ", response.shippingAddress.postalCode)
        assertEquals("NL", response.shippingAddress.countryCodeAlpha2)
        assertEquals("Jon Doe", response.shippingAddress.recipientName)
        assertEquals("Jon", response.givenName)
        assertEquals("Doe", response.surname)
        assertEquals("9KQSUZTL7YZQ4", response.payerId)
        assertEquals("084afbf1db15445587d30bc120a23b09", response.clientMetadataId)
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses LocalPaymentNonce parcels it correctly`() {
        val response = LocalPaymentNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE))

        val parcel = Parcel.obtain().apply {
            response.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<LocalPaymentNonce>().createFromParcel(parcel)
        assertNotNull(parceled)
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", parceled.string)
        assertEquals("jon@getbraintree.com", parceled.email)
        assertEquals("836486 of 22321 Park Lake", parceled.shippingAddress.streetAddress)
        assertEquals("Apt B", parceled.shippingAddress.extendedAddress)
        assertEquals("Den Haag", parceled.shippingAddress.locality)
        assertEquals("CA", parceled.shippingAddress.region)
        assertEquals("2585 GJ", parceled.shippingAddress.postalCode)
        assertEquals("NL", parceled.shippingAddress.countryCodeAlpha2)
        assertEquals("Jon Doe", parceled.shippingAddress.recipientName)
        assertEquals("Jon", parceled.givenName)
        assertEquals("Doe", parceled.surname)
        assertEquals("9KQSUZTL7YZQ4", parceled.payerId)
        assertEquals("084afbf1db15445587d30bc120a23b09", parceled.clientMetadataId)
    }

    @Test
    @Throws(JSONException::class)
    fun `fromJSON parses LocalPaymentNonce and defaults to empty strings for required fields`() {
        val response = LocalPaymentNonce.fromJSON(JSONObject(
            Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_MISSING_FIELDS_RESPONSE)
        )

        assertNotNull(response)
        assertEquals("141b7583-2922-1ce6-1f2e-f352b69115d6", response.string)
        assertNull(response.email)
        assertNull(response.shippingAddress.streetAddress)
        assertNull(response.shippingAddress.extendedAddress)
        assertNull(response.shippingAddress.locality)
        assertNull(response.shippingAddress.region)
        assertNull(response.shippingAddress.postalCode)
        assertNull(response.shippingAddress.countryCodeAlpha2)
        assertNull(response.shippingAddress.recipientName)
        assertEquals("", response.givenName)
        assertEquals("", response.surname)
        assertEquals("", response.payerId)
        assertEquals("c7ce54e0cde5406785b13c99086a9f4c", response.clientMetadataId)
    }
}
