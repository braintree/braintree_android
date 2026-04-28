package com.braintreepayments.api.threedsecure

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureRequestTest {

    @Test
    fun parcels_withAllFieldsPopulated() {
        val original = ThreeDSecureRequest(
            nonce = "fake-nonce",
            amount = "10.00",
            mobilePhoneNumber = "5551234567",
            email = "test@example.com",
            shippingMethod = ThreeDSecureShippingMethod.GROUND,
            billingAddress = ThreeDSecurePostalAddress(
                givenName = "Joe", surname = "Guy",
                streetAddress = "555 Smith St.", locality = "Oakland",
                region = "CA", postalCode = "12345", countryCodeAlpha2 = "US"
            ),
            accountType = ThreeDSecureAccountType.CREDIT,
            additionalInformation = ThreeDSecureAdditionalInformation(
                shippingMethodIndicator = "01", productCode = "AIR"
            ),
            challengeRequested = true,
            dataOnlyRequested = true,
            exemptionRequested = true,
            requestedExemptionType = ThreeDSecureRequestedExemptionType.LOW_VALUE,
            cardAddChallengeRequested = true,
            uiType = ThreeDSecureUiType.NATIVE,
            renderTypes = listOf(
                ThreeDSecureRenderType.OTP, ThreeDSecureRenderType.SINGLE_SELECT,
                ThreeDSecureRenderType.MULTI_SELECT, ThreeDSecureRenderType.OOB
            ),
            customFields = mapOf("field1" to "value1", "field2" to "value2"),
            requestorAppUrl = "https://example.com/app"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original, restored)
    }

    @Test
    fun parcels_withDefaultValues() {
        val original = ThreeDSecureRequest(nonce = "a-nonce", amount = "1.00")

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original, restored)
    }

    @Test
    fun build_producesCorrectJson() {
        val request = ThreeDSecureRequest(
            amount = "10.00",
            mobilePhoneNumber = "5551234567",
            email = "test@example.com",
            shippingMethod = ThreeDSecureShippingMethod.GROUND,
            billingAddress = ThreeDSecurePostalAddress(
                givenName = "Joe", surname = "Guy",
                streetAddress = "555 Smith St.", locality = "Oakland"
            ),
            accountType = ThreeDSecureAccountType.CREDIT,
            challengeRequested = true,
            exemptionRequested = true,
            requestedExemptionType = ThreeDSecureRequestedExemptionType.LOW_VALUE,
            cardAddChallengeRequested = true,
            customFields = mapOf("field1" to "value1")
        )

        val json = JSONObject(request.build("fake-df-ref"))

        assertEquals("10.00", json.getString("amount"))
        assertTrue(json.getBoolean("challenge_requested"))
        assertTrue(json.getBoolean("exemption_requested"))
        assertEquals("low_value", json.getString("requested_exemption_type"))
        assertEquals("credit", json.getString("account_type"))
        assertTrue(json.getBoolean("card_add"))
        assertEquals("fake-df-ref", json.getString("df_reference_id"))
        assertEquals("value1", json.getJSONObject("custom_fields").getString("field1"))

        val info = json.getJSONObject("additional_info")
        assertEquals("5551234567", info.getString("mobile_phone_number"))
        assertEquals("test@example.com", info.getString("email"))
        assertEquals("04", info.getString("shipping_method"))
        assertEquals("Joe", info.getString("billing_given_name"))
        assertEquals("Oakland", info.getString("billing_city"))
    }

    @Test
    fun build_withDefaultValues_producesMinimalJson() {
        val json = JSONObject(ThreeDSecureRequest(amount = "1.00").build(null))

        assertEquals("1.00", json.getString("amount"))
        assertFalse(json.has("account_type"))
        assertFalse(json.has("card_add"))
        assertFalse(json.has("custom_fields"))
        assertFalse(json.has("df_reference_id"))
    }
}
