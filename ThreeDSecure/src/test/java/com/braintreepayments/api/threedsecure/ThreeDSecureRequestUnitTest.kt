package com.braintreepayments.api.threedsecure

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureRequestUnitTest {

    @Suppress("LongMethod")
    @Test
    fun `creates ThreeDSecureAdditionalInformation and parcels it correctly`() {

        val additionalInformation = ThreeDSecureAdditionalInformation(
            accountId = "account-id"
        )
        val billingAddress = ThreeDSecurePostalAddress(
            givenName = "Joe",
            surname = "Guy",
            phoneNumber = "12345678",
            streetAddress = "555 Smith St.",
            extendedAddress = "#5",
            line3 = "Suite C",
            locality = "Oakland",
            region = "CA",
            countryCodeAlpha2 = "US",
            postalCode = "54321"
        )
        val labelCustomization = ThreeDSecureV2LabelCustomization(
            headingTextColor = "#FFA5FF"
        )
        val textBoxCustomization = ThreeDSecureV2TextBoxCustomization(
            borderColor = "#000000"
        )
        val buttonCustomization = ThreeDSecureV2ButtonCustomization(
            textColor = "#FFFFFF",
        )
        val toolbarCustomization = ThreeDSecureV2ToolbarCustomization(
            textColor = "F0F0F0",
            buttonText = "TEST"
        )
        val v2UiCustomization = ThreeDSecureV2UiCustomization(
            labelCustomization = labelCustomization,
            textBoxCustomization = textBoxCustomization,
            buttonCustomization = buttonCustomization,
            buttonType = ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY,
            toolbarCustomization = toolbarCustomization
        )
        val customFields = HashMap<String, String>()
        customFields["custom_key1"] = "custom_value1"
        customFields["custom_key2"] = "custom_value2"
        customFields["custom_key3"] = "custom_value3"

        val expectedRequest = ThreeDSecureRequest(
            nonce = "a-nonce",
            amount = "1.00",
            mobilePhoneNumber = "5151234321",
            email = "tester@example.com",
            shippingMethod = ThreeDSecureShippingMethod.PRIORITY,
            billingAddress = billingAddress,
            additionalInformation = additionalInformation,
            challengeRequested = true,
            dataOnlyRequested = true,
            exemptionRequested = true,
            requestedExemptionType = ThreeDSecureRequestedExemptionType.LOW_VALUE,
            cardAddChallengeRequested = true,
            v2UiCustomization = v2UiCustomization,
            accountType = ThreeDSecureAccountType.CREDIT,
            customFields = customFields
        )

        val parcel = Parcel.obtain().apply {
            expectedRequest.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actualRequest = parcelableCreator<ThreeDSecureRequest>().createFromParcel(parcel)

        assertEquals(expectedRequest.amount, actualRequest.amount)
        assertEquals(expectedRequest.accountType, actualRequest.accountType)
        assertEquals(expectedRequest.nonce, actualRequest.nonce)
        assertEquals(expectedRequest.mobilePhoneNumber, actualRequest.mobilePhoneNumber)
        assertEquals(expectedRequest.email, actualRequest.email)
        assertEquals(expectedRequest.shippingMethod, actualRequest.shippingMethod)
        assertEquals(expectedRequest.billingAddress?.givenName, actualRequest.billingAddress?.givenName)
        assertEquals(expectedRequest.billingAddress?.surname, actualRequest.billingAddress?.surname)
        assertEquals(expectedRequest.billingAddress?.phoneNumber, actualRequest.billingAddress?.phoneNumber)
        assertEquals(expectedRequest.billingAddress?.streetAddress, actualRequest.billingAddress?.streetAddress)
        assertEquals(expectedRequest.billingAddress?.extendedAddress, actualRequest.billingAddress?.extendedAddress)
        assertEquals(expectedRequest.billingAddress?.line3, actualRequest.billingAddress?.line3)
        assertEquals(expectedRequest.billingAddress?.locality, actualRequest.billingAddress?.locality)
        assertEquals(expectedRequest.billingAddress?.region, actualRequest.billingAddress?.region)
        assertEquals(expectedRequest.billingAddress?.countryCodeAlpha2, actualRequest.billingAddress?.countryCodeAlpha2)
        assertEquals(expectedRequest.billingAddress?.postalCode, actualRequest.billingAddress?.postalCode)
        assertEquals(expectedRequest.additionalInformation?.accountId, actualRequest.additionalInformation?.accountId)
        assertEquals(expectedRequest.challengeRequested, actualRequest.challengeRequested)
        assertEquals(expectedRequest.dataOnlyRequested, actualRequest.dataOnlyRequested)
        assertEquals(expectedRequest.exemptionRequested, actualRequest.exemptionRequested)
        assertEquals(expectedRequest.requestedExemptionType, actualRequest.requestedExemptionType)
        assertEquals(expectedRequest.cardAddChallengeRequested, actualRequest.cardAddChallengeRequested)
        assertEquals(expectedRequest.v2UiCustomization?.labelCustomization?.headingTextColor,
                    actualRequest.v2UiCustomization?.labelCustomization?.headingTextColor)
        assertEquals(expectedRequest.v2UiCustomization?.textBoxCustomization?.borderColor,
                    actualRequest.v2UiCustomization?.textBoxCustomization?.borderColor)
        assertEquals(expectedRequest.v2UiCustomization?.buttonCustomization?.backgroundColor,
                    actualRequest.v2UiCustomization?.buttonCustomization?.backgroundColor)
        assertEquals(expectedRequest.v2UiCustomization?.toolbarCustomization?.textColor,
                    actualRequest.v2UiCustomization?.toolbarCustomization?.textColor)
        assertEquals(expectedRequest.v2UiCustomization?.toolbarCustomization?.buttonText,
                    actualRequest.v2UiCustomization?.toolbarCustomization?.buttonText)
        assertEquals(3, actualRequest.customFields?.size)
        assertEquals("custom_value1", actualRequest.customFields?.get("custom_key1"))
        assertEquals("custom_value2", actualRequest.customFields?.get("custom_key2"))
        assertEquals("custom_value3", actualRequest.customFields?.get("custom_key3"))
    }

    @Test
    fun `write to parcel allows cardAddChallengeRequested to be null`() {
        val expectedRequest = ThreeDSecureRequest(
            cardAddChallengeRequested = null
        )
        val parcel = Parcel.obtain().apply {
            expectedRequest.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actualRequest = parcelableCreator<ThreeDSecureRequest>().createFromParcel(parcel)
        assertNull(actualRequest.cardAddChallengeRequested)
    }

    @Suppress("LongMethod")
    @Test
    fun `toJson builds JSONObject correctly from ThreeDSecureRequest`() {
        val additionalInformation = ThreeDSecureAdditionalInformation(
            accountId = "account-id"
        )
        val billingAddress = ThreeDSecurePostalAddress(
            givenName = "billing-given-name",
            surname = "billing-surname",
            phoneNumber = "billing-phone-number",
            streetAddress = "billing-line1",
            extendedAddress = "billing-line2",
            line3 = "billing-line3",
            locality = "billing-city",
            region = "billing-state",
            countryCodeAlpha2 = "billing-country-code",
            postalCode = "billing-postal-code"
        )

        val customFields = HashMap<String, String>()
        customFields["custom_key1"] = "custom_value1"
        customFields["custom_key2"] = "123"

        val request = ThreeDSecureRequest(
            amount = "amount",
            mobilePhoneNumber = "mobile-phone-number",
            email = "email",
            shippingMethod = ThreeDSecureShippingMethod.SAME_DAY,
            billingAddress = billingAddress,
            additionalInformation = additionalInformation,
            challengeRequested = true,
            dataOnlyRequested = true,
            exemptionRequested = true,
            cardAddChallengeRequested = true,
            accountType = ThreeDSecureAccountType.CREDIT,
            customFields = customFields
        )

        val json = JSONObject(request.build("df-reference-id"))
        val additionalInfoJson = json.getJSONObject("additional_info")

        assertEquals("df-reference-id", json.get("df_reference_id"))
        assertEquals("amount", json.get("amount"))
        assertEquals("credit", json.get("account_type"))
        assertTrue(json.getBoolean("card_add"))
        assertTrue(json.getBoolean("challenge_requested"))
        assertTrue(json.getBoolean("exemption_requested"))
        assertTrue(json.getBoolean("data_only_requested"))

        assertEquals("billing-given-name", additionalInfoJson.get("billing_given_name"))
        assertEquals("billing-surname", additionalInfoJson.get("billing_surname"))
        assertEquals("billing-line1", additionalInfoJson.get("billing_line1"))
        assertEquals("billing-line2", additionalInfoJson.get("billing_line2"))
        assertEquals("billing-line3", additionalInfoJson.get("billing_line3"))
        assertEquals("billing-city", additionalInfoJson.get("billing_city"))
        assertEquals("billing-state", additionalInfoJson.get("billing_state"))
        assertEquals("billing-postal-code", additionalInfoJson.get("billing_postal_code"))
        assertEquals("billing-country-code", additionalInfoJson.get("billing_country_code"))
        assertEquals("billing-phone-number", additionalInfoJson.get("billing_phone_number"))
        assertEquals("mobile-phone-number", additionalInfoJson.get("mobile_phone_number"))
        assertEquals("email", additionalInfoJson.get("email"))
        assertEquals("01", additionalInfoJson.get("shipping_method"))
        assertEquals("account-id", additionalInfoJson.get("account_id"))

        val customFieldsJson = json.getJSONObject("custom_fields")
        assertEquals("custom_value1", customFieldsJson.getString("custom_key1"))
        assertEquals("123", customFieldsJson.getString("custom_key2"))
    }

    @Test
    @Throws(JSONException::class)
    fun `toJson does not include accountType when it is not set in ThreeDSecureRequest`() {
        val json = JSONObject(ThreeDSecureRequest().build("df-reference-id"))

        assertFalse(json.has("account_type"))
    }

    @Test
    @Throws(JSONException::class)
    fun `toJson defaults to false when dataOnlyRequested is not set in ThreeDSecureRequest`() {
        val json = JSONObject(ThreeDSecureRequest().build("df-reference-id"))

        assertFalse(json.getBoolean("data_only_requested"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON from ThreeDSecureRequest and contains a dfReferenceId`() {
        val json = JSONObject(ThreeDSecureRequest().build("df-reference-id"))

        assertEquals("df-reference-id", json.getString("df_reference_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 01 when shippingMethod is same day`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.SAME_DAY
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("01", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 02 when shippingMethod is expedited`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.EXPEDITED
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("02", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 03 when shippingMethod is priority`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.PRIORITY
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("03", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 04 when shippingMethod is ground`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.GROUND
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("04", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 05 when shippingMethod is electronic delivery`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.ELECTRONIC_DELIVERY
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("05", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and returns 06 when shippingMethod is ship to store`() {
        val request = ThreeDSecureRequest(
            shippingMethod = ThreeDSecureShippingMethod.SHIP_TO_STORE
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertEquals("06", json.getJSONObject("additional_info").getString("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and does not set shippingMethod if it is not set`() {
        val request = ThreeDSecureRequest()

        val json = JSONObject(request.build("df-reference-id"))
        assertFalse(json.getJSONObject("additional_info").has("shipping_method"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and does not set cardAddChallengeRequested if it is not set`() {
        val request = ThreeDSecureRequest()

        val json = JSONObject(request.build("df-reference-id"))
        assertFalse(json.has("card_add"))
    }

    @Test
    @Throws(JSONException::class)
    fun `builds JSON and sets cardAddChallengeRequested correctly as false`() {
        val request = ThreeDSecureRequest(
            cardAddChallengeRequested = false
        )

        val json = JSONObject(request.build("df-reference-id"))
        assertFalse(json.getBoolean("card_add"))
    }
}
