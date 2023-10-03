package com.braintreepayments.api

import org.robolectric.RobolectricTestRunner
import android.os.Parcel
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PostalAddressUnitTest {
    @Test
    fun constructsCorrectly() {
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
    fun testCanCreatePostalAddress_fromStandardJson() {
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
    fun testCanCreatePostalAddress_fromAlternateJson() {
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
    fun testCanPostalAddressHandleMissingFieldsInJson() {
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
    fun testWriteToParcel_serializesCorrectly() {
        val accountAddressJson = Fixtures.PAYMENT_METHODS_PAYPAL_ADDRESS
        val preSerialized = PostalAddressParser.fromJson(JSONObject(accountAddressJson))
        val parcel = Parcel.obtain()
        preSerialized.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val postSerialized = PostalAddress.CREATOR.createFromParcel(parcel)!!
        assertNotNull(postSerialized)
        assertEquals("123 Fake St.", postSerialized.streetAddress)
        assertEquals("Apt. 3", postSerialized.extendedAddress)
        assertEquals("Oakland", postSerialized.locality)
        assertEquals("CA", postSerialized.region)
        assertEquals("94602", postSerialized.postalCode)
        assertEquals("US", postSerialized.countryCodeAlpha2)
        assertEquals("John Fakerson", postSerialized.recipientName)
    }

    @Test
    fun isEmpty_returnsTrueIfCountryCodeIsNotSet() {
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
    fun isEmpty_returnsFalseIfCountryCodeIsSet() {
        val postalAddress = PostalAddress()
        postalAddress.countryCodeAlpha2 = "US"
        assertFalse(postalAddress.isEmpty)
    }
}
