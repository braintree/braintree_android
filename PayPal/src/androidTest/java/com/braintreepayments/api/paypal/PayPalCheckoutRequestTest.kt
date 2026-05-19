package com.braintreepayments.api.paypal

import android.net.Uri
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
class PayPalCheckoutRequestTest {

    private val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
    private val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)

    @Test
    fun createRequestBody_withMinimalFields_returnsValidJson() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        assertNotNull(result)
        val json = JSONObject(result)
        assertEquals("1.00", json.getString("amount"))
        assertEquals("https://example.com/success", json.getString("return_url"))
        assertEquals("https://example.com/cancel", json.getString("cancel_url"))
        assertEquals("authorize", json.getString("intent"))
        assertFalse(json.getBoolean("offer_paypal_credit"))
        assertFalse(json.getBoolean("offer_pay_later"))
    }

    @Suppress("LongMethod")
    @Test
    fun createRequestBody_withAllFields_returnsValidJson() {
        val shippingAddress = PostalAddress().apply {
            streetAddress = "123 Main St"
            extendedAddress = "Apt 1"
            locality = "Oakland"
            region = "CA"
            postalCode = "94602"
            countryCodeAlpha2 = "US"
            recipientName = "John Doe"
        }

        val request = PayPalCheckoutRequest(
            amount = "10.00",
            hasUserLocationConsent = true,
            intent = PayPalPaymentIntent.SALE,
            currencyCode = "USD",
            shouldRequestBillingAgreement = true,
            shouldOfferPayLater = true,
            shouldOfferCredit = true,
            localeCode = "en_US",
            billingAgreementDescription = "Monthly subscription",
            isShippingAddressRequired = true,
            isShippingAddressEditable = false,
            shippingAddressOverride = shippingAddress,
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN,
            displayName = "Test Merchant",
            merchantAccountId = "merchant_123",
            riskCorrelationId = "risk_123",
            userAuthenticationEmail = "test@example.com",
            userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT,
            lineItems = listOf(
                PayPalLineItem(
                    kind = PayPalLineItemKind.DEBIT,
                    name = "Item 1",
                    quantity = "2",
                    unitAmount = "5.00"
                )
            )
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        assertNotNull(result)
        val json = JSONObject(result)
        assertEquals("10.00", json.getString("amount"))
        assertEquals("sale", json.getString("intent"))
        assertEquals("USD", json.getString("currency_iso_code"))
        assertTrue(json.getBoolean("request_billing_agreement"))
        assertTrue(json.getBoolean("offer_pay_later"))
        assertTrue(json.getBoolean("offer_paypal_credit"))
        assertEquals("test@example.com", json.getString("payer_email"))
        assertEquals("merchant_123", json.getString("merchant_account_id"))
        assertEquals("risk_123", json.getString("correlation_id"))

        val billingDetails = json.getJSONObject("billing_agreement_details")
        assertEquals("Monthly subscription", billingDetails.getString("description"))

        val experienceProfile = json.getJSONObject("experience_profile")
        assertEquals("login", experienceProfile.getString("landing_page_type"))
        assertEquals("Test Merchant", experienceProfile.getString("brand_name"))
        assertEquals("en_US", experienceProfile.getString("locale_code"))
        assertEquals("commit", experienceProfile.getString("user_action"))
        assertFalse(experienceProfile.getBoolean("no_shipping"))
        assertTrue(experienceProfile.getBoolean("address_override"))

        assertEquals("123 Main St", json.getString("line1"))
        assertEquals("Apt 1", json.getString("line2"))
        assertEquals("Oakland", json.getString("city"))
        assertEquals("CA", json.getString("state"))
        assertEquals("94602", json.getString("postal_code"))
        assertEquals("US", json.getString("country_code"))
        assertEquals("John Doe", json.getString("recipient_name"))

        val lineItems = json.getJSONArray("line_items")
        assertEquals(1, lineItems.length())
    }

    @Test
    fun createRequestBody_withLineItems_includesItemsArray() {
        val request = PayPalCheckoutRequest(
            amount = "15.00",
            hasUserLocationConsent = true,
            lineItems = listOf(
                PayPalLineItem(
                    kind = PayPalLineItemKind.DEBIT,
                    name = "Item 1",
                    quantity = "1",
                    unitAmount = "10.00"
                ),
                PayPalLineItem(
                    kind = PayPalLineItemKind.DEBIT,
                    name = "Item 2",
                    quantity = "1",
                    unitAmount = "5.00"
                )
            )
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        val lineItems = json.getJSONArray("line_items")
        assertEquals(2, lineItems.length())

        val item1 = lineItems.getJSONObject(0)
        assertEquals("Item 1", item1.getString("name"))
        assertEquals("1", item1.getString("quantity"))
        assertEquals("10.00", item1.getString("unit_amount"))

        val item2 = lineItems.getJSONObject(1)
        assertEquals("Item 2", item2.getString("name"))
    }

    @Test
    fun createRequestBody_withShippingAddressOverride_includesAddressFields() {
        val address = PostalAddress().apply {
            streetAddress = "836486 of 22321 Park Lake"
            extendedAddress = "Suite 200"
            locality = "Den Haag"
            region = "ZH"
            postalCode = "2585 GJ"
            countryCodeAlpha2 = "NL"
            recipientName = "Jane Smith"
        }

        val request = PayPalCheckoutRequest(
            amount = "1.00",
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
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"))
        assertEquals("Suite 200", json.getString("line2"))
        assertEquals("Den Haag", json.getString("city"))
        assertEquals("ZH", json.getString("state"))
        assertEquals("2585 GJ", json.getString("postal_code"))
        assertEquals("NL", json.getString("country_code"))
        assertEquals("Jane Smith", json.getString("recipient_name"))

        val experienceProfile = json.getJSONObject("experience_profile")
        assertFalse(experienceProfile.getBoolean("address_override"))
    }

    @Test
    fun createRequestBody_withShippingCallbackUrl_includesIt() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            shippingCallbackUrl = Uri.parse("https://example.com/shipping-callback")
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals(
            "https://example.com/shipping-callback",
            json.getString("shipping_callback_url")
        )
    }

    @Test
    fun createRequestBody_withUserPhoneNumber_includesPayerPhone() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            userPhoneNumber = PayPalPhoneNumber(
                countryCode = "1",
                nationalNumber = "4085551234"
            )
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        val payerPhone = json.getJSONObject("payer_phone")
        assertEquals("1", payerPhone.getString("country_code"))
        assertEquals("4085551234", payerPhone.getString("national_number"))
    }

    @Test
    fun createRequestBody_withContactInformation_includesRecipientFields() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            contactInformation = PayPalContactInformation(
                recipientEmail = "recipient@example.com",
                recipentPhoneNumber = PayPalPhoneNumber(
                    countryCode = "44",
                    nationalNumber = "7911123456"
                )
            )
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals("recipient@example.com", json.getString("recipient_email"))
        val recipientPhone = json.getJSONObject("international_phone")
        assertEquals("44", recipientPhone.getString("country_code"))
        assertEquals("7911123456", recipientPhone.getString("national_number"))
    }

    @Test
    fun createRequestBody_withContactPreference_includesPreference() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            contactPreference = PayPalContactPreference.RETAIN_CONTACT_INFORMATION
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals("RETAIN_CONTACT_INFO", json.getString("contact_preference"))
    }

    @Test
    fun createRequestBody_withAppSwitchEnabled_includesAppSwitchParams() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            enablePayPalAppSwitch = true
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            "https://merchant.example.com/applink"
        )

        val json = JSONObject(result)
        assertTrue(json.getBoolean("launch_paypal_app"))
        assertEquals("Android", json.getString("os_type"))
        assertNotNull(json.getString("os_version"))
        assertEquals("https://merchant.example.com/applink", json.getString("merchant_app_return_url"))
    }

    @Test
    fun createRequestBody_withAppSwitchEnabledButNoAppLink_excludesAppSwitchParams() {
        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true,
            enablePayPalAppSwitch = true
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertFalse(json.has("launch_paypal_app"))
        assertFalse(json.has("os_type"))
        assertFalse(json.has("merchant_app_return_url"))
    }

    @Suppress("LongMethod")
    @Test
    fun createRequestBody_withRecurringBillingDetails_includesPlanMetadata() {
        val billingCycle = PayPalBillingCycle(
            isTrial = false,
            numberOfExecutions = 12,
            interval = PayPalBillingInterval.MONTH,
            intervalCount = 1,
            sequence = 1,
            startDate = "2025-01-01",
            pricing = PayPalBillingPricing(
                pricingModel = PayPalPricingModel.FIXED,
                amount = "9.99",
                reloadThresholdAmount = "5.00"
            )
        )

        val trialCycle = PayPalBillingCycle(
            isTrial = true,
            numberOfExecutions = 1,
            interval = PayPalBillingInterval.MONTH,
            intervalCount = 1,
            sequence = 0
        )

        val recurringDetails = PayPalRecurringBillingDetails(
            billingCycles = listOf(trialCycle, billingCycle),
            totalAmount = "119.88",
            currencyISOCode = "USD",
            productName = "Premium Plan",
            oneTimeFeeAmount = "10.00",
            productDescription = "Monthly premium subscription",
            productAmount = "9.99",
            productQuantity = 1,
            shippingAmount = "0.00",
            taxAmount = "1.00"
        )

        val request = PayPalCheckoutRequest(
            amount = "119.88",
            hasUserLocationConsent = true,
            recurringBillingDetails = recurringDetails,
            recurringBillingPlanType = PayPalRecurringBillingPlanType.SUBSCRIPTION
        )

        val result = request.createRequestBody(
            configuration,
            authorization,
            "https://example.com/success",
            "https://example.com/cancel",
            null
        )

        val json = JSONObject(result)
        assertEquals("SUBSCRIPTION", json.getString("plan_type"))
        assertTrue(json.has("plan_metadata"))

        val planMetadata = json.getJSONObject("plan_metadata")
        assertEquals("119.88", planMetadata.getString("total_amount"))
        assertEquals("USD", planMetadata.getString("currency_iso_code"))
        assertEquals("Premium Plan", planMetadata.getString("name"))
        assertEquals("10.00", planMetadata.getString("one_time_fee_amount"))
        assertEquals("Monthly premium subscription", planMetadata.getString("product_description"))
        assertEquals("9.99", planMetadata.getString("product_price"))
        assertEquals(1, planMetadata.getInt("product_quantity"))
        assertEquals("0.00", planMetadata.getString("shipping_amount"))
        assertEquals("1.00", planMetadata.getString("tax_amount"))

        val billingCycles = planMetadata.getJSONArray("billing_cycles")
        assertEquals(2, billingCycles.length())

        val trialJson = billingCycles.getJSONObject(0)
        assertTrue(trialJson.getBoolean("trial"))
        assertEquals(1, trialJson.getInt("number_of_executions"))
        assertEquals(0, trialJson.getInt("sequence"))

        val regularJson = billingCycles.getJSONObject(1)
        assertFalse(regularJson.getBoolean("trial"))
        assertEquals(12, regularJson.getInt("number_of_executions"))
        assertEquals(1, regularJson.getInt("sequence"))
        assertEquals("2025-01-01", regularJson.getString("start_date"))

        val pricingJson = regularJson.getJSONObject("pricing_scheme")
        assertEquals("FIXED", pricingJson.getString("pricing_model"))
        assertEquals("9.99", pricingJson.getString("price"))
        assertEquals("5.00", pricingJson.getString("reload_threshold_amount"))
    }

    @Test
    fun parcels_correctly() {
        val original = PayPalCheckoutRequest(
            amount = "25.00",
            hasUserLocationConsent = true,
            intent = PayPalPaymentIntent.SALE,
            currencyCode = "EUR",
            shouldRequestBillingAgreement = true,
            shouldOfferPayLater = true,
            shouldOfferCredit = true,
            localeCode = "de_DE",
            billingAgreementDescription = "Test description",
            isShippingAddressRequired = true,
            isShippingAddressEditable = true,
            displayName = "My Store",
            merchantAccountId = "merchant_456",
            riskCorrelationId = "risk_456",
            userAuthenticationEmail = "user@example.com",
            userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<PayPalCheckoutRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals("25.00", restored.amount)
        assertTrue(restored.hasUserLocationConsent)
        assertEquals(PayPalPaymentIntent.SALE, restored.intent)
        assertEquals("EUR", restored.currencyCode)
        assertTrue(restored.shouldRequestBillingAgreement)
        assertTrue(restored.shouldOfferPayLater)
        assertTrue(restored.shouldOfferCredit)
        assertEquals("de_DE", restored.localeCode)
        assertEquals("Test description", restored.billingAgreementDescription)
        assertTrue(restored.isShippingAddressRequired)
        assertTrue(restored.isShippingAddressEditable)
        assertEquals("My Store", restored.displayName)
        assertEquals("merchant_456", restored.merchantAccountId)
        assertEquals("risk_456", restored.riskCorrelationId)
        assertEquals("user@example.com", restored.userAuthenticationEmail)
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, restored.userAction)
    }
}
