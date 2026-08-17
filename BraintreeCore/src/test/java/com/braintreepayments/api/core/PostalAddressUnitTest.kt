package com.braintreepayments.api.core

import android.os.Bundle
import com.braintreepayments.api.sharedutils.IntentExtensions.parcelable
import com.braintreepayments.api.testutils.Fixtures
import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PostalAddressUnitTest {
    @Test
    fun `PostalAddress property assignments are readable back correctly`() {
        val postalAddress = PostalAddress().apply {
            streetAddress = "123 Fake St."
            extendedAddress = "Apt. 3"
            locality = "Oakland"
            region = "CA"
            postalCode = "94602"
            countryCodeAlpha2 = "US"
            recipientName = "John Fakerson"
        }
        assertEquals("123 Fake St.", postalAddress.streetAddress)
        assertEquals("Apt. 3", postalAddress.extendedAddress)
        assertEquals("Oakland", postalAddress.locality)
        assertEquals("CA", postalAddress.region)
        assertEquals("94602", postalAddress.postalCode)
        assertEquals("US", postalAddress.countryCodeAlpha2)
        assertEquals("John Fakerson", postalAddress.recipientName)
    }

    @Test
    @Throws(JSONException::class)
    fun `PostalAddressParser fromJson parses standard address json`() {
        val accountAddressJson = Fixtures.PAYMENT_METHODS_PAYPAL_ADDRESS
        val postalAddress = PostalAddressParser.fromJson(JSONObject(accountAddressJson))
        assertEquals("123 Fake St.", postalAddress.streetAddress)
        assertEquals("Apt. 3", postalAddress.extendedAddress)
        assertEquals("Oakland", postalAddress.locality)
        assertEquals("CA", postalAddress.region)
        assertEquals("94602", postalAddress.postalCode)
        assertEquals("US", postalAddress.countryCodeAlpha2)
        assertEquals("John Fakerson", postalAddress.recipientName)
    }

    @Test
    @Throws(JSONException::class)
    fun `PostalAddressParser fromJson parses alternate address json`() {
        val accountAddressJson = Fixtures.PAYMENT_METHODS_PAYPAL_ADDRESS_ALTERNATE
        val postalAddress = PostalAddressParser.fromJson(JSONObject(accountAddressJson))
        assertEquals("123 Fake St.", postalAddress.streetAddress)
        assertEquals("Apt. 3", postalAddress.extendedAddress)
        assertEquals("Oakland", postalAddress.locality)
        assertEquals("CA", postalAddress.region)
        assertEquals("94602", postalAddress.postalCode)
        assertEquals("US", postalAddress.countryCodeAlpha2)
        assertEquals("John Fakerson", postalAddress.recipientName)
    }

    @Test
    @Throws(JSONException::class)
    fun `when json is missing address fields, PostalAddressParser fromJson leaves fields null`() {
        val accountAddressJson = Fixtures.RANDOM_JSON
        val postalAddress = PostalAddressParser.fromJson(JSONObject(accountAddressJson))
        assertNull(postalAddress.streetAddress)
        assertNull(postalAddress.extendedAddress)
        assertNull(postalAddress.locality)
        assertNull(postalAddress.region)
        assertNull(postalAddress.postalCode)
        assertNull(postalAddress.countryCodeAlpha2)
    }

    @Test
    @Throws(JSONException::class)
    fun `PostalAddress survives Parcel serialization round trip`() {
        val accountAddressJson = Fixtures.PAYMENT_METHODS_PAYPAL_ADDRESS
        val preSerialized = PostalAddressParser.fromJson(JSONObject(accountAddressJson))

        val bundle = Bundle().apply {
            putParcelable("TEST_PARCEL", preSerialized)
        }
        val postSerialized: PostalAddress? = bundle.parcelable("TEST_PARCEL")
        assertNotNull(postSerialized)
        assertEquals("123 Fake St.", postSerialized!!.streetAddress)
        assertEquals("Apt. 3", postSerialized.extendedAddress)
        assertEquals("Oakland", postSerialized.locality)
        assertEquals("CA", postSerialized.region)
        assertEquals("94602", postSerialized.postalCode)
        assertEquals("US", postSerialized.countryCodeAlpha2)
        assertEquals("John Fakerson", postSerialized.recipientName)
    }

    @Test
    fun `when countryCodeAlpha2 is not set, isEmpty returns true`() {
        val postalAddress = PostalAddress().apply {
            streetAddress = "123 Fake St."
            extendedAddress = "Apt. 3"
            locality = "Oakland"
            region = "CA"
            postalCode = "94602"
            recipientName = "John Fakerson"
        }
        assertTrue(postalAddress.isEmpty)
    }

    @Test
    fun `when countryCodeAlpha2 is set, isEmpty returns false`() {
        val postalAddress = PostalAddress()
        postalAddress.countryCodeAlpha2 = "US"
        assertFalse(postalAddress.isEmpty)
    }
}
