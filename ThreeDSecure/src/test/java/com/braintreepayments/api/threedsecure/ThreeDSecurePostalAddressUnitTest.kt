package com.braintreepayments.api.threedsecure

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecurePostalAddressUnitTest {

    @Test
    fun `ThreeDSecurePostalAddress constructs correctly`() {
        val postalAddress = ThreeDSecurePostalAddress(
            streetAddress = "123 Fake St.",
            extendedAddress = "Apt. 3",
            locality = "Oakland",
            region = "CA",
            postalCode = "94602",
            countryCodeAlpha2 = "US",
            givenName = "John",
            surname = "Fakerson",
            phoneNumber = "5151231234"
        )

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
    fun `ThreeDSecurePostalAddress constructs and parcels correctly`() {
        val postalAddress = ThreeDSecurePostalAddress(
            streetAddress = "123 Fake St.",
            extendedAddress = "Apt. 3",
            locality = "Oakland",
            region = "CA",
            postalCode = "94602",
            countryCodeAlpha2 = "US",
            givenName = "John",
            surname = "Fakerson",
            phoneNumber = "5151231234"
        )

        val parcel = Parcel.obtain().apply {
            postalAddress.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<ThreeDSecurePostalAddress>().createFromParcel(parcel)

        assertNotNull(parceled)
        assertEquals("123 Fake St.", parceled.streetAddress)
        assertEquals("Apt. 3", parceled.extendedAddress)
        assertEquals("Oakland", parceled.locality)
        assertEquals("CA", parceled.region)
        assertEquals("94602", parceled.postalCode)
        assertEquals("US", parceled.countryCodeAlpha2)
        assertEquals("John", parceled.givenName)
        assertEquals("Fakerson", parceled.surname)
        assertEquals("5151231234", parceled.phoneNumber)
    }

    @Test
    @Throws(JSONException::class)
    fun `toJson builds from ThreeDSecurePostalAddress with all parameters set correctly`() {
        val postalAddress = ThreeDSecurePostalAddress(
            streetAddress = "123 Fake St.",
            extendedAddress = "Apt. 3",
            line3 = "Suite C",
            locality = "Oakland",
            region = "CA",
            postalCode = "94602",
            countryCodeAlpha2 = "US",
            givenName = "John",
            surname = "Fakerson",
            phoneNumber = "5151231234"
        )

        val jsonParams = postalAddress.toJson()
        val jsonBillingAddress = jsonParams.getJSONObject("billingAddress")

        assertEquals("John", jsonParams.get("firstName"))
        assertEquals("Fakerson", jsonParams.get("lastName"))
        assertEquals("5151231234", jsonParams.get("phoneNumber"))
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"))
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"))
        assertEquals("Suite C", jsonBillingAddress.get("line3"))
        assertEquals("Oakland", jsonBillingAddress.get("city"))
        assertEquals("CA", jsonBillingAddress.get("state"))
        assertEquals("94602", jsonBillingAddress.get("postalCode"))
        assertEquals("US", jsonBillingAddress.get("countryCode"))
    }

    @Test
    @Throws(JSONException::class)
    fun `toJson builds from ThreeDSecurePostalAddress with partial parameters`() {
        val postalAddress = ThreeDSecurePostalAddress(
            streetAddress = "123 Fake St.",
            extendedAddress = "Apt. 3",
            line3 = "Suite C",
            locality = "Oakland",
            region = "CA",
            postalCode = "94602",
            givenName = "John",
            surname = "Fakerson",
        )

        val jsonParams = postalAddress.toJson()
        val jsonBillingAddress = jsonParams.getJSONObject("billingAddress")

        assertEquals("John", jsonParams.get("firstName"))
        assertEquals("Fakerson", jsonParams.get("lastName"))
        assertTrue(jsonParams.isNull("phoneNumber"))
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"))
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"))
        assertEquals("Oakland", jsonBillingAddress.get("city"))
        assertEquals("CA", jsonBillingAddress.get("state"))
        assertEquals("94602", jsonBillingAddress.get("postalCode"))
        assertTrue(jsonBillingAddress.isNull("countryCode"))
    }

    @Test
    fun `toJson builds from ThreeDSecurePostalAddress with empty parameters`() {
        val postalAddress = ThreeDSecurePostalAddress()

        val jsonParams = postalAddress.toJson()

        assertTrue(jsonParams.isNull("billingAddress"))
        assertTrue(jsonParams.isNull("firstName"))
        assertTrue(jsonParams.isNull("lastName"))
        assertTrue(jsonParams.isNull("phoneNumber"))
    }
}
