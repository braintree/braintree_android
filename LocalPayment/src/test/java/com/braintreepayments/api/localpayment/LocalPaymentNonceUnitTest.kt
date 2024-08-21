package com.braintreepayments.api.localpayment

import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import com.braintreepayments.api.testutils.Fixtures
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentNonceUnitTest {
    @Test
    @Throws(JSONException::class)
    fun fromJson_parsesResponse() {
        val result = fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE))

        assertNotNull(result)
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", result.string)
        assertEquals("jon@getbraintree.com", result.email)
        assertEquals("836486 of 22321 Park Lake", result.shippingAddress.streetAddress)
        assertEquals("Apt B", result.shippingAddress.extendedAddress)
        assertEquals("Den Haag", result.shippingAddress.locality)
        assertEquals("CA", result.shippingAddress.region)
        assertEquals("2585 GJ", result.shippingAddress.postalCode)
        assertEquals("NL", result.shippingAddress.countryCodeAlpha2)
        assertEquals("Jon Doe", result.shippingAddress.recipientName)
        assertEquals("Jon", result.givenName)
        assertEquals("Doe", result.surname)
        assertEquals("9KQSUZTL7YZQ4", result.payerId)
        assertEquals("084afbf1db15445587d30bc120a23b09", result.clientMetadataId)
    }

    @Test
    @Throws(JSONException::class)
    fun parcelsCorrectly() {
        val result = fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE))

        val parceled: LocalPaymentNonce = result

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
}
