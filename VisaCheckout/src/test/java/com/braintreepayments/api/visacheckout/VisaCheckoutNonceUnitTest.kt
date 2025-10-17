package com.braintreepayments.api.visacheckout

import android.os.Parcel
import com.braintreepayments.api.card.BinType
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutNonceUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `creates VisaCheckoutNonce from JSON successfully`() {
        val visaCheckoutNonce = VisaCheckoutNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE)
        )

        assertEquals("11", visaCheckoutNonce.lastTwo)
        assertEquals("Visa", visaCheckoutNonce.cardType)
        assertEquals("123456-12345-12345-a-adfa", visaCheckoutNonce.string)
        assertFalse(visaCheckoutNonce.isDefault)
        assertEquals("callId", visaCheckoutNonce.callId)

        assertNotNull(visaCheckoutNonce.billingAddress)
        assertEquals("billingFirstName", visaCheckoutNonce.billingAddress.firstName)
        assertEquals("billingLastName", visaCheckoutNonce.billingAddress.lastName)
        assertEquals("billingStreetAddress", visaCheckoutNonce.billingAddress.streetAddress)
        assertEquals("billingExtendedAddress", visaCheckoutNonce.billingAddress.extendedAddress)
        assertEquals("billingLocality", visaCheckoutNonce.billingAddress.locality)
        assertEquals("billingRegion", visaCheckoutNonce.billingAddress.region)
        assertEquals("billingPostalCode", visaCheckoutNonce.billingAddress.postalCode)
        assertEquals("billingCountryCode", visaCheckoutNonce.billingAddress.countryCode)

        assertNotNull(visaCheckoutNonce.shippingAddress)
        assertEquals("shippingFirstName", visaCheckoutNonce.shippingAddress.firstName)
        assertEquals("shippingLastName", visaCheckoutNonce.shippingAddress.lastName)
        assertEquals("shippingStreetAddress", visaCheckoutNonce.shippingAddress.streetAddress)
        assertEquals("shippingExtendedAddress", visaCheckoutNonce.shippingAddress.extendedAddress)
        assertEquals("shippingLocality", visaCheckoutNonce.shippingAddress.locality)
        assertEquals("shippingRegion", visaCheckoutNonce.shippingAddress.region)
        assertEquals("shippingPostalCode", visaCheckoutNonce.shippingAddress.postalCode)
        assertEquals("shippingCountryCode", visaCheckoutNonce.shippingAddress.countryCode)

        assertNotNull(visaCheckoutNonce.userData)
        assertEquals("userFirstName", visaCheckoutNonce.userData.userFirstName)
        assertEquals("userLastName", visaCheckoutNonce.userData.userLastName)
        assertEquals("userFullName", visaCheckoutNonce.userData.userFullName)
        assertEquals("userUserName", visaCheckoutNonce.userData.username)
        assertEquals("userEmail", visaCheckoutNonce.userData.userEmail)

        assertNotNull(visaCheckoutNonce.binData)
        assertEquals(BinType.Unknown, visaCheckoutNonce.binData.prepaid)
        assertEquals(BinType.Yes, visaCheckoutNonce.binData.healthcare)
        assertEquals(BinType.No, visaCheckoutNonce.binData.debit)
        assertEquals(BinType.Unknown, visaCheckoutNonce.binData.durbinRegulated)
        assertEquals(BinType.Unknown, visaCheckoutNonce.binData.commercial)
        assertEquals(BinType.Unknown, visaCheckoutNonce.binData.payroll)
        assertEquals(BinType.Unknown.name, visaCheckoutNonce.binData.issuingBank)
        assertEquals("Something", visaCheckoutNonce.binData.countryOfIssuance)
        assertEquals("123", visaCheckoutNonce.binData.productId)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VisaCheckoutNonce with empty callId from JSON when no callId is present`() {
        val visaCheckoutResponseJson = JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE)

        val visaCheckoutCardsJson = visaCheckoutResponseJson.getJSONArray("visaCheckoutCards")
        val visaCheckoutNonceJson = visaCheckoutCardsJson.getJSONObject(0).apply {
            remove("callId")
        }
        visaCheckoutCardsJson.put(0, visaCheckoutNonceJson)
        visaCheckoutResponseJson.put("visaCheckoutCards", visaCheckoutCardsJson)

        val visaCheckoutNonce = VisaCheckoutNonce.fromJSON(visaCheckoutResponseJson)

        assertEquals("", visaCheckoutNonce.callId)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates VisaCheckoutNonce from JSOn and parcels it correctly`() {
        val visaCheckoutNonce = VisaCheckoutNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE)
        )

        val parcel = Parcel.obtain().apply {
            visaCheckoutNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actual = parcelableCreator<VisaCheckoutNonce>().createFromParcel(parcel)

        assertEquals(visaCheckoutNonce.string, actual.string)
        assertEquals(visaCheckoutNonce.isDefault, actual.isDefault)
        assertEquals(visaCheckoutNonce.lastTwo, actual.lastTwo)
        assertEquals(visaCheckoutNonce.cardType, actual.cardType)
        assertEquals(visaCheckoutNonce.binData, actual.binData)
        assertVisaCheckoutAddress(visaCheckoutNonce.billingAddress, actual.billingAddress)
        assertVisaCheckoutAddress(visaCheckoutNonce.shippingAddress, actual.shippingAddress)
        assertEquals(visaCheckoutNonce.callId, actual.callId)
        assertEquals(visaCheckoutNonce.userData.userFirstName, actual.userData.userFirstName)
        assertEquals(visaCheckoutNonce.userData.userLastName, actual.userData.userLastName)
        assertEquals(visaCheckoutNonce.userData.userFullName, actual.userData.userFullName)
        assertEquals(visaCheckoutNonce.userData.userEmail, actual.userData.userEmail)
    }

    fun assertVisaCheckoutAddress(expected: VisaCheckoutAddress, actual:VisaCheckoutAddress) {
        assertEquals(expected.firstName, actual.firstName)
        assertEquals(expected.lastName, actual.lastName)
        assertEquals(expected.streetAddress, actual.streetAddress)
        assertEquals(expected.extendedAddress, actual.extendedAddress)
        assertEquals(expected.locality, actual.locality)
        assertEquals(expected.region, actual.region)
        assertEquals(expected.postalCode, actual.postalCode)
        assertEquals(expected.countryCode, actual.countryCode)
        assertEquals(expected.phoneNumber, actual.phoneNumber)
    }
}
