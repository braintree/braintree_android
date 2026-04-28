package com.braintreepayments.api.paypal

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PayPalVaultRequestTest {

    private val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
    private val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)

    @Test
    fun createRequestBody_withMinimalFields_returnsValidJson() {
        val request = PayPalVaultRequest(hasUserLocationConsent = true)

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        assertNotNull(result)
        val json = JSONObject(result)
        assertEquals("https://example.com/success", json.getString("return_url"))
        assertEquals("https://example.com/cancel", json.getString("cancel_url"))
        assertFalse(json.getBoolean("offer_paypal_credit"))
        assertFalse(json.has("amount"))
    }

    @Test
    fun createRequestBody_withBillingAgreementDescription_includesDescription() {
        val request = PayPalVaultRequest(
            hasUserLocationConsent = true,
            billingAgreementDescription = "Monthly subscription"
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals("Monthly subscription", json.getString("description"))
    }

    @Test
    fun createRequestBody_withShippingAddress_includesShippingAddressObject() {
        val address = PostalAddress().apply {
            streetAddress = "123 Main St"
            extendedAddress = "Apt 1"
            locality = "Oakland"
            region = "CA"
            postalCode = "94602"
            countryCodeAlpha2 = "US"
            recipientName = "John Doe"
        }

        val request = PayPalVaultRequest(
            hasUserLocationConsent = true,
            shippingAddressOverride = address,
            isShippingAddressRequired = true,
            isShippingAddressEditable = true
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertTrue(json.has("shipping_address"))
        val shippingJson = json.getJSONObject("shipping_address")
        assertEquals("123 Main St", shippingJson.getString("line1"))
        assertEquals("Apt 1", shippingJson.getString("line2"))
        assertEquals("Oakland", shippingJson.getString("city"))
        assertEquals("CA", shippingJson.getString("state"))
        assertEquals("94602", shippingJson.getString("postal_code"))
        assertEquals("US", shippingJson.getString("country_code"))
        assertEquals("John Doe", shippingJson.getString("recipient_name"))

        val experienceProfile = json.getJSONObject("experience_profile")
        assertFalse(experienceProfile.getBoolean("address_override"))
    }

    @Test
    fun createRequestBody_withUserAuthenticationEmail_includesPayerEmail() {
        val request = PayPalVaultRequest(
            hasUserLocationConsent = true,
            userAuthenticationEmail = "test@example.com"
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals("test@example.com", json.getString("payer_email"))
    }

    @Test
    fun createRequestBody_withShouldOfferCredit_includesOfferCredit() {
        val request = PayPalVaultRequest(
            hasUserLocationConsent = true,
            shouldOfferCredit = true
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertTrue(json.getBoolean("offer_paypal_credit"))
    }

    @Test
    fun parcels_correctly() {
        val original = PayPalVaultRequest(
            hasUserLocationConsent = true,
            shouldOfferCredit = true,
            localeCode = "fr_FR",
            billingAgreementDescription = "Vault description",
            isShippingAddressRequired = true,
            isShippingAddressEditable = true,
            displayName = "My Vault Store",
            merchantAccountId = "vault_merchant",
            riskCorrelationId = "vault_risk",
            userAuthenticationEmail = "vault@example.com",
            userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<PayPalVaultRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertTrue(restored.hasUserLocationConsent)
        assertTrue(restored.shouldOfferCredit)
        assertEquals("fr_FR", restored.localeCode)
        assertEquals("Vault description", restored.billingAgreementDescription)
        assertTrue(restored.isShippingAddressRequired)
        assertTrue(restored.isShippingAddressEditable)
        assertEquals("My Vault Store", restored.displayName)
        assertEquals("vault_merchant", restored.merchantAccountId)
        assertEquals("vault_risk", restored.riskCorrelationId)
        assertEquals("vault@example.com", restored.userAuthenticationEmail)
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, restored.userAction)
    }
}
