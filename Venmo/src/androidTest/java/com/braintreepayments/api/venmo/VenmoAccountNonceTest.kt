package com.braintreepayments.api.venmo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoAccountNonceTest {

    @Test
    fun fromJSON_withVenmoAccountResponse_parsesNonceAndUsername() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertEquals("fake-venmo-nonce", nonce.string)
        assertTrue(nonce.isDefault)
        assertEquals("venmojoe", nonce.username)
    }

    @Test
    fun fromJSON_withVenmoAccountResponse_hasNullPayerInfo() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertNull(nonce.email)
        assertNull(nonce.externalId)
        assertNull(nonce.firstName)
        assertNull(nonce.lastName)
        assertNull(nonce.phoneNumber)
        assertNull(nonce.billingAddress)
        assertNull(nonce.shippingAddress)
    }

    @Test
    fun fromJSON_withPaymentMethodContext_parsesNonceAndUsername() {
        val json = JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertEquals("sample-payment-method-id", nonce.string)
        assertFalse(nonce.isDefault)
        assertEquals("@sampleuser", nonce.username)
    }

    @Test
    fun fromJSON_withPaymentMethodContext_parsesPayerInfo() {
        val json = JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertEquals("venmo-email", nonce.email)
        assertEquals("venmo-external-id", nonce.externalId)
        assertEquals("venmo-first-name", nonce.firstName)
        assertEquals("venmo-last-name", nonce.lastName)
        assertEquals("venmo-phone-number", nonce.phoneNumber)
    }

    @Test
    fun fromJSON_withPaymentMethodContextWithAddresses_parsesAddresses() {
        val json = JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON_WITH_ADDRESSES)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertNotNull(nonce.billingAddress)
        assertNotNull(nonce.shippingAddress)
    }

    @Test
    fun fromJSON_withNullPayerInfo_returnsNullPayerFields() {
        val json = JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertEquals("sample-payment-method-id", nonce.string)
        assertEquals("@sampleuser", nonce.username)
        assertNull(nonce.email)
        assertNull(nonce.externalId)
        assertNull(nonce.firstName)
        assertNull(nonce.lastName)
        assertNull(nonce.phoneNumber)
        assertNull(nonce.billingAddress)
        assertNull(nonce.shippingAddress)
    }

    @Test
    fun fromJSON_withPlainVenmoObject_parsesCorrectly() {
        val json = JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT)

        val nonce = VenmoAccountNonce.fromJSON(json)

        assertEquals("fake-venmo-nonce", nonce.string)
        assertEquals("happy-venmo-joe", nonce.username)
        assertFalse(nonce.isDefault)
    }
}
