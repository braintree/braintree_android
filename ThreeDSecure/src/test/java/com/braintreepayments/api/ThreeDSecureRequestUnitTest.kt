package com.braintreepayments.api

import android.os.Parcel
import junit.framework.TestCase.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureRequestUnitTest {

    @Test
    fun constructor_noVersionRequested_defaultsToVersion2() {
        val request = ThreeDSecureRequest()

        assertEquals(ThreeDSecureRequest.VERSION_2, request.versionRequested)
    }

    @Test
    fun writeToParcel() {
        val additionalInformation = ThreeDSecureAdditionalInformation()
        additionalInformation.accountId = "account-id"

        val billingAddress = ThreeDSecurePostalAddress()
        billingAddress.givenName = "Joe"
        billingAddress.surname = "Guy"
        billingAddress.phoneNumber = "12345678"
        billingAddress.streetAddress = "555 Smith St."
        billingAddress.extendedAddress = "#5"
        billingAddress.line3 = "Suite C"
        billingAddress.locality = "Oakland"
        billingAddress.region = "CA"
        billingAddress.countryCodeAlpha2 = "US"
        billingAddress.postalCode = "54321"

        val labelCustomization = ThreeDSecureV2LabelCustomization()
        labelCustomization.headingTextColor = "#FFA5FF"

        val v2UiCustomization = ThreeDSecureV2UiCustomization()
        v2UiCustomization.labelCustomization = labelCustomization

        val v1UiCustomization = ThreeDSecureV1UiCustomization()
        v1UiCustomization.redirectButtonText = "return-button-text"
        v1UiCustomization.redirectDescription = "return-label-text"

        val expected = ThreeDSecureRequest()
        expected.nonce = "a-nonce"
        expected.amount = "1.00"
        expected.mobilePhoneNumber = "5151234321"
        expected.email = "tester@example.com"
        expected.shippingMethod = ThreeDSecureShippingMethod.PRIORITY
        expected.versionRequested = ThreeDSecureRequest.VERSION_2
        expected.billingAddress = billingAddress
        expected.additionalInformation = additionalInformation
        expected.isChallengeRequested = true
        expected.isDataOnlyRequested = true
        expected.isExemptionRequested = true
        expected.v2UiCustomization = v2UiCustomization
        expected.v1UiCustomization = v1UiCustomization
        expected.accountType = ThreeDSecureRequest.CREDIT

        val parcel = Parcel.obtain()
        expected.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val actual = ThreeDSecureRequest(parcel)

        assertEquals(expected.amount, actual.amount)
        assertEquals(expected.accountType, actual.accountType)
        assertEquals(expected.nonce, actual.nonce)
        assertEquals(expected.mobilePhoneNumber, actual.mobilePhoneNumber)
        assertEquals(expected.email, actual.email)
        assertEquals(expected.shippingMethod, actual.shippingMethod)
        assertEquals(expected.versionRequested, actual.versionRequested)
        assertEquals(expected.billingAddress!!.givenName, actual.billingAddress!!.givenName)
        assertEquals(expected.billingAddress!!.surname, actual.billingAddress!!.surname)
        assertEquals(expected.billingAddress!!.phoneNumber, actual.billingAddress!!.phoneNumber)
        assertEquals(expected.billingAddress!!.streetAddress, actual.billingAddress!!.streetAddress)
        assertEquals(expected.billingAddress!!.extendedAddress, actual.billingAddress!!.extendedAddress)
        assertEquals(expected.billingAddress!!.line3, actual.billingAddress!!.line3)
        assertEquals(expected.billingAddress!!.locality, actual.billingAddress!!.locality)
        assertEquals(expected.billingAddress!!.region, actual.billingAddress!!.region)
        assertEquals(expected.billingAddress!!.countryCodeAlpha2, actual.billingAddress!!.countryCodeAlpha2)
        assertEquals(expected.billingAddress!!.postalCode, actual.billingAddress!!.postalCode)
        assertEquals(expected.additionalInformation!!.accountId, actual.additionalInformation!!.accountId)
        assertEquals(expected.isChallengeRequested, actual.isChallengeRequested)
        assertEquals(expected.isDataOnlyRequested, actual.isDataOnlyRequested)
        assertEquals(expected.isExemptionRequested, actual.isExemptionRequested)
        assertEquals(expected.v2UiCustomization!!.labelCustomization!!.headingTextColor,
                actual.v2UiCustomization!!.labelCustomization!!.headingTextColor)
        assertEquals(expected.v1UiCustomization!!.redirectButtonText,
                actual.v1UiCustomization!!.redirectButtonText)
        assertEquals(expected.v1UiCustomization!!.redirectDescription,
                actual.v1UiCustomization!!.redirectDescription)
    }

    @Test
    @Throws(JSONException::class)
    fun toJson() {
        val additionalInformation = ThreeDSecureAdditionalInformation()
        additionalInformation.accountId = "account-id"

        val billingAddress = ThreeDSecurePostalAddress()
        billingAddress.givenName = "billing-given-name"
        billingAddress.surname = "billing-surname"
        billingAddress.streetAddress = "billing-line1"
        billingAddress.extendedAddress = "billing-line2"
        billingAddress.line3 = "billing-line3"
        billingAddress.locality = "billing-city"
        billingAddress.region = "billing-state"
        billingAddress.postalCode = "billing-postal-code"
        billingAddress.countryCodeAlpha2 = "billing-country-code"
        billingAddress.phoneNumber = "billing-phone-number"

        val request = ThreeDSecureRequest()
        request.versionRequested = ThreeDSecureRequest.VERSION_2
        request.amount = "amount"
        request.mobilePhoneNumber = "mobile-phone-number"
        request.email = "email"
        request.shippingMethod = ThreeDSecureShippingMethod.SAME_DAY
        request.billingAddress = billingAddress
        request.additionalInformation = additionalInformation
        request.isChallengeRequested = true
        request.isDataOnlyRequested = true
        request.isExemptionRequested = true
        request.accountType = ThreeDSecureRequest.CREDIT

        val json = JSONObject(request.build("df-reference-id"))
        val additionalInfoJson = json.getJSONObject("additional_info")

        assertEquals("df-reference-id", json["df_reference_id"])
        assertEquals("amount", json["amount"])
        assertEquals("credit", json["account_type"])
        assertTrue(json.getBoolean("challenge_requested"))
        assertTrue(json.getBoolean("exemption_requested"))
        assertTrue(json.getBoolean("data_only_requested"))
        assertEquals("billing-given-name", additionalInfoJson["billing_given_name"])
        assertEquals("billing-surname", additionalInfoJson["billing_surname"])
        assertEquals("billing-line1", additionalInfoJson["billing_line1"])
        assertEquals("billing-line2", additionalInfoJson["billing_line2"])
        assertEquals("billing-line3", additionalInfoJson["billing_line3"])
        assertEquals("billing-city", additionalInfoJson["billing_city"])
        assertEquals("billing-state", additionalInfoJson["billing_state"])
        assertEquals("billing-postal-code", additionalInfoJson["billing_postal_code"])
        assertEquals("billing-country-code", additionalInfoJson["billing_country_code"])
        assertEquals("billing-phone-number", additionalInfoJson["billing_phone_number"])
        assertEquals("mobile-phone-number", additionalInfoJson["mobile_phone_number"])
        assertEquals("email", additionalInfoJson["email"])
        assertEquals("01", additionalInfoJson["shipping_method"])
        assertEquals("account-id", additionalInfoJson["account_id"])
    }

    @Test
    @Throws(JSONException::class)
    fun toJson_whenAccountTypeNotSet_doesNotIncludeAccountType() {
        val json = JSONObject(ThreeDSecureRequest()
                .build("df-reference-id"))

        assertFalse(json.has("account_type"))
    }

    @Test
    @Throws(JSONException::class)
    fun toJson_whenDataOnlyRequestedNotSet_defaultsToFalse() {
        val json = JSONObject(ThreeDSecureRequest()
                .build("df-reference-id"))

        assertFalse(json.getBoolean("data_only_requested"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_withVersion1_doesNotContainDfReferenceId() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.versionRequested = ThreeDSecureRequest.VERSION_1

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertFalse(json.has("df_reference_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_withVersion2_containsDfReferenceId() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.versionRequested = ThreeDSecureRequest.VERSION_2

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("df-reference-id", json.getString("df_reference_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsSameDay_returns01() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.SAME_DAY

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("01", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsExpedited_returns02() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.EXPEDITED

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("02", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsPriority_returns03() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.PRIORITY

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("03", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsGround_returns04() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.GROUND

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("04", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsElectronicDelivery_returns05() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("05", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsShipToStore_returns06() {
        val threeDSecureRequest = ThreeDSecureRequest()
        threeDSecureRequest.shippingMethod = ThreeDSecureShippingMethod.SHIP_TO_STORE

        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertEquals("06", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun build_whenShippingMethodIsNotSet_doesNotSetShippingMethod() {
        val threeDSecureRequest = ThreeDSecureRequest()
        val json = JSONObject(threeDSecureRequest.build("df-reference-id"))

        assertFalse(json.getJSONObject("additional_info").has("shipping_method"))
    }
}