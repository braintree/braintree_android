package com.braintreepayments.api.googlepay

import android.os.Parcel
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.sharedutils.Json
import com.braintreepayments.api.testutils.Assertions.assertBinDataEqual
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GooglePayCardNonceUnitTest {

    private fun `parse PostalAddress from JSON`(address: JSONObject): PostalAddress {
        val result = PostalAddress().apply {
            recipientName = Json.optString(address, "name", "")
            streetAddress = Json.optString(address, "address1", "")
            extendedAddress = listOf(
                Json.optString(address, "address2", ""),
                Json.optString(address, "address3", "")
            ).joinToString(separator = "\n").trim()
            locality = Json.optString(address, "locality", "")
            region = Json.optString(address, "administrativeArea", "")
            countryCodeAlpha2 = Json.optString(address, "countryCode", "")
            postalCode = Json.optString(address, "postalCode", "")
        }
        return result
    }

    private fun `assert PostalAddress`(expected: PostalAddress, actual: PostalAddress) {
        assertEquals(expected.toString(), actual.toString())
    }


    @Test
    @Throws(Exception::class)
    fun `parses JSON object and creates GooglePayCardNonce`() {
        val response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE
        val billing  = JSONObject(response)
            .getJSONObject("paymentMethodData")
            .getJSONObject("info")
            .getJSONObject("billingAddress")
        val shipping = JSONObject(response).getJSONObject("shippingAddress")

        val billingPostalAddress = `parse PostalAddress from JSON`(billing)
        val shippingPostalAddress = `parse PostalAddress from JSON`(shipping)

        val googlePayCardNonce = GooglePayCardNonce.fromJSON(JSONObject(response)) as GooglePayCardNonce

        assertEquals("fake-google-pay-nonce", googlePayCardNonce.string)
        assertEquals("Visa", googlePayCardNonce.cardType)
        assertEquals("123456", googlePayCardNonce.bin)
        assertEquals("11", googlePayCardNonce.lastTwo)
        assertEquals("1234", googlePayCardNonce.lastFour)
        assertEquals("android-user@example.com", googlePayCardNonce.email)
        `assert PostalAddress`(billingPostalAddress, googlePayCardNonce.billingAddress)
        `assert PostalAddress`(shippingPostalAddress, googlePayCardNonce.shippingAddress)
        assertTrue { googlePayCardNonce.isNetworkTokenized }
        assertEquals("VISA", googlePayCardNonce.cardNetwork)
    }

    @Test
    @Throws(Exception::class)
    fun `parses JSON object without BillingAddress and creates GooglePayCardNonce`() {
        var response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE
        val responseNoBillingAddress = JSONObject(response).apply {
            getJSONObject("paymentMethodData")
                .getJSONObject("info").remove("billingAddress")
        }
        response = responseNoBillingAddress.toString()

        val billing = JSONObject()
        val billingPostalAddress = `parse PostalAddress from JSON`(billing)
        val googlePayCardNonce = GooglePayCardNonce.fromJSON(JSONObject(response)) as GooglePayCardNonce

        `assert PostalAddress`(billingPostalAddress, googlePayCardNonce.billingAddress)
    }

    @Test
    @Throws(Exception::class)
    fun `parses JSON object without ShippingAddress and creates GooglePayCardNonce`() {
        var response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE
        val responseNoShippingAddress = JSONObject(response).apply {
            remove("shippingAddress")
        }
        response = responseNoShippingAddress.toString()

        val shipping = JSONObject()
        val shippingPostalAddress = `parse PostalAddress from JSON`(shipping)
        val googlePayCardNonce = GooglePayCardNonce.fromJSON(JSONObject(response)) as GooglePayCardNonce

        `assert PostalAddress`(shippingPostalAddress, googlePayCardNonce.shippingAddress)
    }

    @Test
    @Throws(JSONException::class)
    fun `parses JSON object without email and creates GooglePayCardNonce`() {
        var response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE
        val responseNoEmail = JSONObject(response).apply {
            remove("email")
        }
        response = responseNoEmail.toString()

        val googlePayCardNonce = GooglePayCardNonce.fromJSON(JSONObject(response)) as GooglePayCardNonce

        assertEquals("", googlePayCardNonce.email)
    }

    @Test
    @Throws(Exception::class)
    fun `parcels and returns all properties correctly`() {
        val response = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE
        val billing  = JSONObject(response)
            .getJSONObject("paymentMethodData")
            .getJSONObject("info")
            .getJSONObject("billingAddress")
        val shipping = JSONObject(response).getJSONObject("shippingAddress")

        val billingPostalAddress = `parse PostalAddress from JSON`(billing)
        val shippingPostalAddress = `parse PostalAddress from JSON`(shipping)

        val googlePayCardNonce = GooglePayCardNonce.fromJSON(JSONObject(response)) as GooglePayCardNonce
        val parcel = Parcel.obtain().apply {
            googlePayCardNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<GooglePayCardNonce>().createFromParcel(parcel)
        assertEquals("fake-google-pay-nonce", googlePayCardNonce.string)
        assertEquals("Visa", googlePayCardNonce.cardType)
        assertEquals("11", googlePayCardNonce.lastTwo)
        assertEquals("1234", googlePayCardNonce.lastFour)
        assertEquals("android-user@example.com", googlePayCardNonce.email)
        `assert PostalAddress`(billingPostalAddress, googlePayCardNonce.billingAddress)
        `assert PostalAddress`(shippingPostalAddress, googlePayCardNonce.shippingAddress)
        assertTrue { googlePayCardNonce.isNetworkTokenized }
        assertEquals("VISA", googlePayCardNonce.cardNetwork)

        assertBinDataEqual(googlePayCardNonce.binData, parceled.binData)
    }
}
