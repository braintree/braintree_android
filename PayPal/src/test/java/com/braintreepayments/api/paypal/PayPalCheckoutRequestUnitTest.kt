package com.braintreepayments.api.paypal

import android.net.Uri
import android.os.Parcel
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.PostalAddress
import com.google.testing.junit.testparameterinjector.TestParameter
import io.mockk.mockk
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import java.util.ArrayList

@RunWith(RobolectricTestParameterInjector::class)
class PayPalCheckoutRequestUnitTest {

    @OptIn(ExperimentalBetaApi::class)
    @Test
    fun `successfully creates PayPalCheckoutRequest with default values`() {
        val request = PayPalCheckoutRequest("1.00", false)
        assertNull(request.shopperSessionId)
        assertNotNull(request.amount)
        assertNull(request.currencyCode)
        assertNull(request.localeCode)
        assertFalse(request.isShippingAddressRequired)
        assertNull(request.shippingAddressOverride)
        assertNull(request.displayName)
        assertEquals(PayPalPaymentIntent.AUTHORIZE, request.intent)
        assertNull(request.landingPageType)
        assertNull(request.billingAgreementDescription)
        assertFalse(request.shouldOfferPayLater)
        assertFalse(request.enablePayPalAppSwitch)
        assertNull(request.userAuthenticationEmail)
        assertFalse(request.hasUserLocationConsent)
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    fun `creates PayPalCheckoutRequest and sets its values successfully`(
        @TestParameter appSwitchEnabled: Boolean
    ) {
        val postalAddress = PostalAddress()
        val request = PayPalCheckoutRequest("1.00", true)
        request.apply {
            shopperSessionId = "shopper-insights-id"
            currencyCode = "USD"
            shouldOfferPayLater = true
            intent = PayPalPaymentIntent.SALE
            localeCode = "US"
            shouldRequestBillingAgreement = true
            billingAgreementDescription = "Billing Agreement Description"
            isShippingAddressRequired = true
            shippingAddressOverride = postalAddress
            userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT
            displayName = "Display Name"
            riskCorrelationId = "123-correlation"
            enablePayPalAppSwitch = appSwitchEnabled
            userAuthenticationEmail = "test-email"
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
        }

        assertEquals("shopper-insights-id", request.shopperSessionId)
        assertEquals("1.00", request.amount)
        assertEquals("USD", request.currencyCode)
        assertEquals("US", request.localeCode)
        assertTrue(request.shouldRequestBillingAgreement)
        assertEquals("Billing Agreement Description", request.billingAgreementDescription)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(postalAddress, request.shippingAddressOverride)
        assertEquals(PayPalPaymentIntent.SALE, request.intent)
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, request.userAction)
        assertEquals("Display Name", request.displayName)
        assertEquals("123-correlation", request.riskCorrelationId)
        assertEquals(appSwitchEnabled, request.enablePayPalAppSwitch)
        assertEquals("test-email", request.userAuthenticationEmail)
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, request.landingPageType)
        assertTrue(request.shouldOfferPayLater)
        assertTrue(request.hasUserLocationConsent)
    }

    @Test
    fun `parcels PayPalCheckoutRequest correctly`(
        @TestParameter appSwitchEnabled: Boolean
    ) {
        val postalAddress = PostalAddress()
        postalAddress.apply {
            recipientName = "Postal Address"
        }

        val request = PayPalCheckoutRequest("12.34", true)
        request.apply {
            currencyCode = "USD"
            localeCode = "en-US"
            billingAgreementDescription = "Billing Agreement Description"
            isShippingAddressRequired = true
            isShippingAddressEditable = true
            shippingAddressOverride = postalAddress
            shouldOfferPayLater = true
            intent = PayPalPaymentIntent.SALE
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
            userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT
            displayName = "Display Name"
            riskCorrelationId = "123-correlation"
            merchantAccountId = "merchant_account_id"
            enablePayPalAppSwitch = appSwitchEnabled
            userAuthenticationEmail = "test-email"
        }
        val lineItems = ArrayList<PayPalLineItem>()
        lineItems.add(PayPalLineItem(PayPalLineItemKind.DEBIT, "An Item", "1", "1"))
        request.lineItems = lineItems

        val parcel = Parcel.obtain().apply {
            request.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<PayPalCheckoutRequest>().createFromParcel(parcel)
        assertEquals("12.34", parceled.amount)
        assertEquals("USD", parceled.currencyCode)
        assertEquals("en-US", parceled.localeCode)
        assertEquals("Billing Agreement Description", parceled.billingAgreementDescription)
        assertTrue(parceled.isShippingAddressRequired)
        assertTrue(parceled.isShippingAddressEditable)
        assertEquals(appSwitchEnabled, parceled.enablePayPalAppSwitch)
        assertEquals("test-email", parceled.userAuthenticationEmail)
        assertEquals("Postal Address", parceled.shippingAddressOverride?.recipientName)
        assertEquals(PayPalPaymentIntent.SALE, parceled.intent)
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, parceled.landingPageType)
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, parceled.userAction)
        assertEquals(postalAddress, parceled.shippingAddressOverride)
        assertEquals("Display Name", parceled.displayName)
        assertEquals("123-correlation", parceled.riskCorrelationId)
        assertEquals("merchant_account_id", parceled.merchantAccountId)
        assertEquals(1, parceled.lineItems.size)
        assertEquals("An Item", parceled.lineItems[0].name)
        assertTrue(parceled.hasUserLocationConsent)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets userAuthenticationEmail when not null`() {
        val payerEmail = "payer_email@example.com"
        val request = PayPalCheckoutRequest("1.00", true).apply {
            userAuthenticationEmail = payerEmail
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"payer_email\":" + "\"" + payerEmail + "\""))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and does not set userAuthenticationEmail when it is empty`() {
        val payerEmail = ""
        val request = PayPalCheckoutRequest("1.00", true).apply {
            userAuthenticationEmail = payerEmail
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertFalse(requestBody.contains("\"payer_email\":" + "\"" + payerEmail + "\""))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets shippingCallbackUri when not null`() {
        val urlString = "https://www.example.com/path"
        val uri = Uri.parse(urlString)
        val request = PayPalCheckoutRequest("1.00", true).apply {
            shippingCallbackUrl = uri
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = "universal_url"
        )

        val jsonObject = JSONObject(requestBody)
        assertEquals(urlString, jsonObject.getString("shipping_callback_url"))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets appSwitchParameters regardless of userAuthenticationEmail value`(
        @TestParameter(value = ["", "some@email.com"]) payerEmail: String
    ) {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            enablePayPalAppSwitch = true
            userAuthenticationEmail = payerEmail
        }
        val appLink = "universal_url"

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = appLink
        )

        val jsonObject = JSONObject(requestBody)
        assertTrue(jsonObject.getBoolean("launch_paypal_app"))
        assertEquals("Android", jsonObject.getString("os_type"))
        assertEquals(appLink, jsonObject.getString("merchant_app_return_url"))
        assertNotNull(jsonObject.getString("os_version"))
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets shopperInsightsSessionId`() {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            shopperSessionId = "shopper-insights-id"
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = "universal_url"
        )

        assertTrue(requestBody.contains("\"shopper_session_id\":" + "\"shopper-insights-id\""))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and does not set shippingCallbackUri when null`() {
        val request = PayPalCheckoutRequest("1.00", true)

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        val jsonObject = JSONObject(requestBody)
        assertFalse(jsonObject.has("shipping_callback_url"))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and does not set shippingCallbackUri when empty`() {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            shippingCallbackUrl = Uri.parse("")
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        val jsonObject = JSONObject(requestBody)
        assertFalse(jsonObject.has("shipping_callback_url"))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets userPhoneNumber when not null`() {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            userPhoneNumber = PayPalPhoneNumber("1", "1231231234")
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"payer_phone\":{\"country_code\":\"1\",\"national_number\":\"1231231234\"}"))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets contactInformation when not null`() {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            contactInformation = PayPalContactInformation("some@email.com", PayPalPhoneNumber("1", "1234567890"))
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"recipient_email\":\"some@email.com\""))
        assertTrue(requestBody.contains(
            "\"international_phone\":{\"country_code\":\"1\",\"national_number\":\"1234567890\"}"
            )
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and sets contactPreference when not null`() {
        val request = PayPalCheckoutRequest("1.00", true).apply {
            contactPreference = PayPalContactPreference.UPDATE_CONTACT_INFORMATION
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"contact_preference\":\"UPDATE_CONTACT_INFO\""))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates requestBody and does not set contactPreference when null`() {
        val request = PayPalCheckoutRequest("1.00", true)

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertFalse(requestBody.contains("contact_preference"))
    }
}
