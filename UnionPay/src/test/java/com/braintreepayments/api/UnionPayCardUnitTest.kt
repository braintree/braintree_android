package com.braintreepayments.api

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnionPayCardUnitTest {

    @Test
    fun getApiPath_returnsExpected() {
        assertEquals("credit_cards", UnionPayCard().apiPath)
    }

    @Test
    @Throws(JSONException::class)
    fun cardNumber_addsToJson() {
        val sut = UnionPayCard()
        sut.number = "myCardNumber"

        assertEquals("myCardNumber", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("number"))
    }

    @Test
    @Throws(JSONException::class)
    fun expirationMonth_addsToJson() {
        val sut = UnionPayCard()
        sut.expirationMonth = "12"

        assertEquals("12", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationMonth"))
    }

    @Test
    @Throws(JSONException::class)
    fun expirationYear_addsToJson() {
        val sut = UnionPayCard()
        sut.expirationYear = "2020"

        assertEquals("2020", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("expirationYear"))
    }

    @Test
    @Throws(JSONException::class)
    fun expirationDate_addsToJsonAsExpirationMonthAndExpirationYear() {
        val sut = UnionPayCard()
        sut.expirationDate = "12/2020"

        val enrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment")

        assertEquals("12", enrollment.getString("expirationMonth"))
        assertEquals("2020", enrollment.getString("expirationYear"))
    }

    @Test
    @Throws(JSONException::class)
    fun mobileCountryCode_addsToJson() {
        val sut = UnionPayCard()
        sut.mobileCountryCode = "1"

        assertEquals("1", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileCountryCode"))
    }

    @Test
    @Throws(JSONException::class)
    fun mobilePhoneNumber_addsToJson() {
        val sut = UnionPayCard()
        sut.mobilePhoneNumber = "867-5309"

        assertEquals("867-5309", sut.buildEnrollment()
                .getJSONObject("unionPayEnrollment")
                .getString("mobileNumber"))
    }

    @Test
    @Throws(JSONException::class)
    fun smsCode_addsToOptionsJson() {
        val sut = UnionPayCard()
        sut.smsCode = "mySmsCode"

        val jsonObject = sut.buildJSON()

        assertEquals(
            "mySmsCode", jsonObject?.getJSONObject("creditCard")
                ?.getJSONObject("options")
                ?.getJSONObject("unionPayEnrollment")
                ?.getString("smsCode")
        )
    }

    @Test
    @Throws(JSONException::class)
    fun enrollmentId_addsToOptionsJson() {
        val sut = UnionPayCard()
        sut.enrollmentId = "myEnrollmentId"

        val jsonObject = sut.buildJSON()

        assertEquals(
            "myEnrollmentId", jsonObject?.getJSONObject("creditCard")
                ?.getJSONObject("options")
                ?.getJSONObject("unionPayEnrollment")
                ?.getString("id")
        )
    }

    @Test
    @Throws(JSONException::class)
    fun doesNotIncludeEmptyStrings() {
        val sut = UnionPayCard()
        sut.number = ""
        sut.expirationDate = ""
        sut.expirationMonth = ""
        sut.expirationYear = ""
        sut.cvv = ""
        sut.postalCode = ""
        sut.cardholderName = ""
        sut.firstName = ""
        sut.lastName = ""
        sut.streetAddress = ""
        sut.locality = ""
        sut.postalCode = ""
        sut.region = ""
        sut.enrollmentId = ""
        sut.mobileCountryCode = ""
        sut.mobilePhoneNumber = ""
        sut.smsCode = ""

        val json = sut.buildJSON()

        assertEquals(
            "{\"options\":{\"unionPayEnrollment\":{}}}",
            json?.getJSONObject(BaseCard.CREDIT_CARD_KEY).toString()
        )
        assertFalse(json?.has(BaseCard.BILLING_ADDRESS_KEY) == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildEnrollment_createsUnionPayEnrollmentJson() {
        val sut = UnionPayCard()
        sut.cvv = "123"
        sut.enrollmentId = "enrollment-id"
        sut.expirationYear = "expiration-year"
        sut.expirationMonth = "expiration-month"
        sut.number = "card-number"
        sut.mobileCountryCode = "mobile-country-code"
        sut.mobilePhoneNumber = "mobile-phone-number"
        sut.smsCode = "sms-code"
        sut.integration = "test-integration"
        sut.source = "test-source"
        sut.sessionId = "test-session-id"

        val unionPayEnrollment = sut.buildEnrollment().getJSONObject("unionPayEnrollment")

        assertEquals("card-number", unionPayEnrollment.getString("number"))
        assertEquals("expiration-month", unionPayEnrollment.getString("expirationMonth"))
        assertEquals("expiration-year", unionPayEnrollment.getString("expirationYear"))
        assertEquals("mobile-country-code", unionPayEnrollment.getString("mobileCountryCode"))
        assertEquals("mobile-phone-number", unionPayEnrollment.getString("mobileNumber"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_createsUnionPayTokenizeJson() {
        val sut = UnionPayCard()
        sut.cvv = "123"
        sut.enrollmentId = "enrollment-id"
        sut.expirationYear = "expiration-year"
        sut.expirationMonth = "expiration-month"
        sut.number = "card-number"
        sut.mobileCountryCode = "mobile-country-code"
        sut.mobilePhoneNumber = "mobile-phone-number"
        sut.smsCode = "sms-code"
        sut.integration = "test-integration"
        sut.source = "test-source"
        sut.sessionId = "test-session-id"
        val tokenizePayload = sut.buildJSON()
        val creditCard = tokenizePayload?.getJSONObject("creditCard")

        assertEquals("card-number", creditCard?.getString("number"))
        assertEquals("expiration-month", creditCard?.getString("expirationMonth"))
        assertEquals("expiration-year", creditCard?.getString("expirationYear"))
        assertEquals("123", creditCard?.getString("cvv"))

        val options = creditCard?.getJSONObject("options")
        val unionPayEnrollment = options?.getJSONObject("unionPayEnrollment")

        assertEquals("enrollment-id", unionPayEnrollment?.getString("id"))
        assertEquals("sms-code", unionPayEnrollment?.getString("smsCode"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_doesNotIncludeValidate() {
        val unionPayCard = UnionPayCard()

        val unionPayOptions = unionPayCard.buildJSON()
            ?.getJSONObject("creditCard")
            ?.getJSONObject("options")

        assertFalse(unionPayOptions?.has("validate") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun build_standardPayload() {
        val sut = UnionPayCard()
        sut.number = "someCardNumber"
        sut.expirationMonth = "expirationMonth"
        sut.expirationYear = "expirationYear"
        sut.cvv = "cvv"
        sut.enrollmentId = "enrollmentId"
        sut.smsCode = "smsCode"

        val tokenizePayload = sut.buildJSON()
        val creditCardPayload = tokenizePayload?.getJSONObject("creditCard")
        val optionsPayload = creditCardPayload?.getJSONObject("options")
        val unionPayEnrollmentPayload = optionsPayload?.getJSONObject("unionPayEnrollment")

        assertEquals("someCardNumber", creditCardPayload?.getString("number"))
        assertEquals("expirationMonth", creditCardPayload?.getString("expirationMonth"))
        assertEquals("expirationYear", creditCardPayload?.getString("expirationYear"))
        assertEquals("cvv", creditCardPayload?.getString("cvv"))
        assertFalse(optionsPayload?.has("validate") == true)
        assertEquals("enrollmentId", unionPayEnrollmentPayload?.getString("id"))
        assertEquals("smsCode", unionPayEnrollmentPayload?.getString("smsCode"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_optionalSmsCode() {
        val sut = UnionPayCard()
        sut.number = "someCardNumber"
        sut.expirationMonth = "expirationMonth"
        sut.expirationYear = "expirationYear"
        sut.cvv = "cvv"
        sut.enrollmentId = "enrollmentId"

        val tokenizePayload = sut.buildJSON()
        val creditCardPayload = tokenizePayload?.getJSONObject("creditCard")
        val optionsPayload = creditCardPayload?.getJSONObject("options")
        val unionPayEnrollmentPayload = optionsPayload?.getJSONObject("unionPayEnrollment")

        assertEquals("someCardNumber", creditCardPayload?.getString("number"))
        assertEquals("expirationMonth", creditCardPayload?.getString("expirationMonth"))
        assertEquals("expirationYear", creditCardPayload?.getString("expirationYear"))
        assertEquals("cvv", creditCardPayload?.getString("cvv"))
        assertFalse(optionsPayload?.has("validate") == true)
        assertEquals("enrollmentId", unionPayEnrollmentPayload?.getString("id"))
        assertFalse(unionPayEnrollmentPayload?.has("smsCode") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun build_doesNotIncludeCvv() {
        val sut = UnionPayCard()
        sut.number= "some-card-number"
        sut.cvv = "123"

        val unionPayEnrollmentPayload = sut.buildJSON()

        assertFalse(unionPayEnrollmentPayload?.has("cvv") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildEnrollment_basicPayload() {
        val sut = UnionPayCard()
        sut.number = "someCardNumber"
        sut.expirationMonth = "expirationMonth"
        sut.expirationYear = "expirationYear"
        sut.mobileCountryCode = "mobileCountryCode"
        sut.mobilePhoneNumber = "mobilePhoneNumber"

        val result = JSONObject(sut.buildEnrollment().toString())
        val unionPayEnrollment = result.getJSONObject("unionPayEnrollment")

        assertEquals("someCardNumber", unionPayEnrollment.getString("number"))
        assertEquals("expirationMonth", unionPayEnrollment.getString("expirationMonth"))
        assertEquals("expirationYear", unionPayEnrollment.getString("expirationYear"))
        assertEquals("mobileCountryCode", unionPayEnrollment.getString("mobileCountryCode"))
        assertEquals("mobilePhoneNumber", unionPayEnrollment.getString("mobileNumber"))
    }
}
