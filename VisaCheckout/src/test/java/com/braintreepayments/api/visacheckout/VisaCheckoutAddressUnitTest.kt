package com.braintreepayments.api.visacheckout

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutAddressUnitTest {
    private lateinit var sampleAddress: JSONObject

    @BeforeTest
    fun setup() {
        sampleAddress = JSONObject()
            .put("firstName", "firstName")
            .put("lastName", "lastName")
            .put("streetAddress", "streetAddress")
            .put("extendedAddress", "extendedAddress")
            .put("locality", "locality")
            .put("region", "region")
            .put("postalCode", "postalCode")
            .put("countryCode", "countryCode")
            .put("phoneNumber", "phoneNumber");
    }

    @Test
    fun `fromJson when valid returns populated object`() {
        val visaCheckoutAddress = VisaCheckoutAddress.fromJson(sampleAddress)

        assertEquals("firstName", visaCheckoutAddress.firstName)
        assertEquals("lastName", visaCheckoutAddress.lastName)
        assertEquals("streetAddress", visaCheckoutAddress.streetAddress)
        assertEquals("extendedAddress", visaCheckoutAddress.extendedAddress)
        assertEquals("locality", visaCheckoutAddress.locality)
        assertEquals("region", visaCheckoutAddress.region)
        assertEquals("postalCode", visaCheckoutAddress.postalCode)
        assertEquals("countryCode", visaCheckoutAddress.countryCode)
        assertEquals("phoneNumber", visaCheckoutAddress.phoneNumber)
    }

    @Test
    fun `fromJson when null returns empty object`() {
        val visaCheckoutAddress = VisaCheckoutAddress.fromJson(null)

        assertEquals("", visaCheckoutAddress.firstName)
        assertEquals("", visaCheckoutAddress.lastName)
        assertEquals("", visaCheckoutAddress.streetAddress)
        assertEquals("", visaCheckoutAddress.extendedAddress)
        assertEquals("", visaCheckoutAddress.locality)
        assertEquals("", visaCheckoutAddress.region)
        assertEquals("", visaCheckoutAddress.postalCode)
        assertEquals("", visaCheckoutAddress.countryCode)
        assertEquals("", visaCheckoutAddress.phoneNumber)
    }

    @Test
    fun `parcels correctly`() {
        val visaCheckoutAddress = VisaCheckoutAddress.fromJson(sampleAddress)

        val parcel = Parcel.obtain().apply {
            visaCheckoutAddress.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val result = parcelableCreator<VisaCheckoutAddress>().createFromParcel(parcel)

        assertEquals(visaCheckoutAddress.firstName, result.firstName)
        assertEquals(visaCheckoutAddress.lastName, result.lastName)
        assertEquals(visaCheckoutAddress.streetAddress, result.streetAddress)
        assertEquals(visaCheckoutAddress.extendedAddress, result.extendedAddress)
        assertEquals(visaCheckoutAddress.locality, result.locality)
        assertEquals(visaCheckoutAddress.region, result.region)
        assertEquals(visaCheckoutAddress.postalCode, result.postalCode)
        assertEquals(visaCheckoutAddress.countryCode, result.countryCode)
        assertEquals(visaCheckoutAddress.phoneNumber, result.phoneNumber)
    }
}
