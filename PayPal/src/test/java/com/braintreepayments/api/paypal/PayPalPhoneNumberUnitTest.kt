package com.braintreepayments.api.paypal

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalPhoneNumberUnitTest {

    @Test
    fun `when toJson is called, a JSONObject is returned with the correct properties`() {
        val subject = PayPalPhoneNumber(
            countryCode = "1",
            nationalNumber = "1231231234"
        )
        val result = subject.toJson()

        assertEquals("1", result.get("country_code"))
        assertEquals("1231231234", result.get("national_number"))
    }
}
