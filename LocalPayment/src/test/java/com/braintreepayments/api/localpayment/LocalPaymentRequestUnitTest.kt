package com.braintreepayments.api.localpayment

import android.net.Uri
import com.braintreepayments.api.core.PostalAddress
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentRequestUnitTest {
    @Test
    @Throws(JSONException::class)
    fun build_setsAllParams() {
        val address = PostalAddress()
        address.streetAddress = "836486 of 22321 Park Lake"
        address.extendedAddress = "Apt 2"
        address.countryCodeAlpha2 = "NL"
        address.locality = "Den Haag"
        address.region = "CA"
        address.postalCode = "2585 GJ"

        val request = LocalPaymentRequest(true,
            address,
            "1.10",
            "bank-id-code",
            "EUR",
            "My Brand!",
            "jon@getbraintree.com",
            "Jon",
            "local-merchant-account-id",
            "ideal",
            "NL",
            "639847934",
            false,
            "Doe"
        )

        assertTrue(request.hasUserLocationConsent)

        val json = JSONObject(request.build("http://success-url.com", "http://cancel-url.com"))

        assertEquals("sale", json.getString("intent"))
        assertEquals("1.10", json.getString("amount"))

        assertEquals("sale", json.getString("intent"))
        assertEquals("Jon", json.getString("firstName"))
        assertEquals("Doe", json.getString("lastName"))
        assertEquals("639847934", json.getString("phone"))
        assertEquals("EUR", json.getString("currencyIsoCode"))
        assertEquals("ideal", json.getString("fundingSource"))
        assertEquals("jon@getbraintree.com", json.getString("payerEmail"))
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"))
        assertEquals("CA", json.getString("state"))
        assertEquals("Apt 2", json.getString("line2"))
        assertEquals("Den Haag", json.getString("city"))
        assertEquals("2585 GJ", json.getString("postalCode"))
        assertEquals("NL", json.getString("countryCode"))
        assertEquals("local-merchant-account-id", json.getString("merchantAccountId"))
        assertEquals("NL", json.getString("paymentTypeCountryCode"))
        assertEquals("bank-id-code", json.getString("bic"))
        assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"))
        assertEquals(
            "My Brand!",
            json.getJSONObject("experienceProfile").getString("brandName")
        )
        val expectedCancelUrl = Uri.parse("http://cancel-url.com").toString()
        val expectedReturnUrl = Uri.parse("http://success-url.com").toString()
        assertEquals(expectedCancelUrl, json.getString("cancelUrl"))
        assertEquals(expectedReturnUrl, json.getString("returnUrl"))
    }
}
