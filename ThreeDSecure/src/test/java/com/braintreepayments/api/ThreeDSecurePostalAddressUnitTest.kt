package com.braintreepayments.api

import android.os.Parcel
import junit.framework.TestCase.*
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecurePostalAddressUnitTest {

    @Test
    fun constructsCorrectly() {
        val postalAddress = ThreeDSecurePostalAddress()
        postalAddress.streetAddress = "123 Fake St."
        postalAddress.extendedAddress = "Apt. 3"
        postalAddress.locality = "Oakland"
        postalAddress.region = "CA"
        postalAddress.postalCode = "94602"
        postalAddress.countryCodeAlpha2 = "US"
        postalAddress.givenName = "John"
        postalAddress.surname = "Fakerson"
        postalAddress.phoneNumber = "5151231234"

        assertEquals("123 Fake St.", postalAddress.streetAddress)
        assertEquals("Apt. 3", postalAddress.extendedAddress)
        assertEquals("Oakland", postalAddress.locality)
        assertEquals("CA", postalAddress.region)
        assertEquals("94602", postalAddress.postalCode)
        assertEquals("US", postalAddress.countryCodeAlpha2)
        assertEquals("John", postalAddress.givenName)
        assertEquals("Fakerson", postalAddress.surname)
        assertEquals("5151231234", postalAddress.phoneNumber)
    }

    @Test
    fun testWriteToParcel_serializesCorrectly() {
        val preSerialized = ThreeDSecurePostalAddress()
        preSerialized.streetAddress = "123 Fake St."
        preSerialized.extendedAddress = "Apt. 3"
        preSerialized.line3 = "Suite A"
        preSerialized.locality = "Oakland"
        preSerialized.region = "CA"
        preSerialized.postalCode = "94602"
        preSerialized.countryCodeAlpha2 = "US"
        preSerialized.givenName = "John"
        preSerialized.surname = "Fakerson"
        preSerialized.phoneNumber = "5151231234"
        val parcel = Parcel.obtain()
        preSerialized.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val postSerialized = ThreeDSecurePostalAddress.CREATOR.createFromParcel(parcel)
        assertNotNull(postSerialized)
        assertEquals("123 Fake St.", postSerialized.streetAddress)
        assertEquals("Apt. 3", postSerialized.extendedAddress)
        assertEquals("Suite A", postSerialized.line3)
        assertEquals("Oakland", postSerialized.locality)
        assertEquals("CA", postSerialized.region)
        assertEquals("94602", postSerialized.postalCode)
        assertEquals("US", postSerialized.countryCodeAlpha2)
        assertEquals("John", postSerialized.givenName)
        assertEquals("Fakerson", postSerialized.surname)
        assertEquals("5151231234", postSerialized.phoneNumber)
    }

    @Test
    @Throws(JSONException::class)
    fun testToJson_buildsAllParameters() {
        val address = ThreeDSecurePostalAddress()
        address.streetAddress = "123 Fake St."
        address.extendedAddress = "Apt. 3"
        address.line3 = "Suite C"
        address.locality = "Oakland"
        address.region = "CA"
        address.postalCode = "94602"
        address.countryCodeAlpha2 = "US"
        address.givenName = "John"
        address.surname = "Fakerson"
        address.phoneNumber = "5151231234"

        val jsonParams = address.toJson()
        val jsonBillingAddress = jsonParams.getJSONObject("billingAddress")

        assertEquals("John", jsonParams["firstName"])
        assertEquals("Fakerson", jsonParams["lastName"])
        assertEquals("5151231234", jsonParams["phoneNumber"])
        assertEquals("123 Fake St.", jsonBillingAddress["line1"])
        assertEquals("Apt. 3", jsonBillingAddress["line2"])
        assertEquals("Suite C", jsonBillingAddress["line3"])
        assertEquals("Oakland", jsonBillingAddress["city"])
        assertEquals("CA", jsonBillingAddress["state"])
        assertEquals("94602", jsonBillingAddress["postalCode"])
        assertEquals("US", jsonBillingAddress["countryCode"])
    }

    @Test
    @Throws(JSONException::class)
    fun testToJson_buildsPartialParameters() {
        val address = ThreeDSecurePostalAddress()
        address.streetAddress = "123 Fake St."
        address.extendedAddress = "Apt. 3"
        address.locality = "Oakland"
        address.region = "CA"
        address.postalCode = "94602"
        address.givenName = "John"
        address.surname = "Fakerson"

        val jsonParams = address.toJson()
        val jsonBillingAddress = jsonParams.getJSONObject("billingAddress")

        assertEquals("John", jsonParams["firstName"])
        assertEquals("Fakerson", jsonParams["lastName"])
        assertTrue(jsonParams.isNull("phoneNumber"))
        assertEquals("123 Fake St.", jsonBillingAddress["line1"])
        assertEquals("Apt. 3", jsonBillingAddress["line2"])
        assertEquals("Oakland", jsonBillingAddress["city"])
        assertEquals("CA", jsonBillingAddress["state"])
        assertEquals("94602", jsonBillingAddress["postalCode"])
        assertTrue(jsonBillingAddress.isNull("countryCode"))
    }

    @Test
    fun testToJson_buildsEmptyParameters() {
        val address = ThreeDSecurePostalAddress()
        val jsonParams = address.toJson()

        assertTrue(jsonParams.isNull("billingAddress"))
        assertTrue(jsonParams.isNull("firstName"))
        assertTrue(jsonParams.isNull("lastName"))
        assertTrue(jsonParams.isNull("phoneNumber"))
    }
}