package com.braintreepayments.api.paypal
import android.os.Build
import android.os.Parcel
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import com.google.testing.junit.testparameterinjector.TestParameter
import io.mockk.mockk
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestParameterInjector::class)
class PayPalVaultRequestUnitTest {

    @OptIn(ExperimentalBetaApi::class)
    @Test
    fun `creates new PayPalVaultRequest and set default values correctly`() {
        val request = PayPalVaultRequest(false)

        assertNull(request.shopperSessionId)
        assertNull(request.localeCode)
        assertFalse(request.isShippingAddressRequired)
        assertNull(request.shippingAddressOverride)
        assertNull(request.displayName)
        assertNull(request.landingPageType)
        assertFalse(request.shouldOfferCredit)
        assertFalse(request.hasUserLocationConsent)
        assertFalse(request.enablePayPalAppSwitch)
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Suppress("LongMethod")
    fun `creates PayPalVaultRequest and sets values correctly`() {
        val postalAddress = PostalAddress()
        val request = PayPalVaultRequest(true).apply {
            shopperSessionId = "shopper-insights-id"
            localeCode = "US"
            billingAgreementDescription = "Billing Agreement Description"
            isShippingAddressRequired = true
            shippingAddressOverride = postalAddress
            displayName = "Display Name"
            riskCorrelationId = "123-correlation"
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
            shouldOfferCredit = true
        }
        val billingInterval = PayPalBillingInterval.MONTH
        val pricingModel = PayPalPricingModel.FIXED
        val billingPricing = PayPalBillingPricing(pricingModel, "1.00").apply {
            reloadThresholdAmount = "6.00"
        }
        val billingCycle = PayPalBillingCycle(true, 2, billingInterval, 1)
            .apply {
                sequence = 1
                startDate = "2024-04-06T00:00:00Z"
                pricing = billingPricing
            }
        val billingDetails = PayPalRecurringBillingDetails(listOf(billingCycle), "11.00", "USD")
            .apply {
                oneTimeFeeAmount = "2.00"
                productName = "A Product"
                productDescription = "A Description"
                productQuantity = 1
                shippingAmount = "5.00"
                taxAmount = "3.00"
            }
        request.apply {
            recurringBillingDetails = billingDetails
            recurringBillingPlanType = PayPalRecurringBillingPlanType.RECURRING
        }
        assertEquals("shopper-insights-id", request.shopperSessionId)
        assertEquals("US", request.localeCode)
        assertEquals("Billing Agreement Description", request.billingAgreementDescription)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(postalAddress, request.shippingAddressOverride)
        assertEquals("Display Name", request.displayName)
        assertEquals("123-correlation", request.riskCorrelationId)
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, request.landingPageType)
        assertTrue(request.shouldOfferCredit)
        assertTrue(request.hasUserLocationConsent)
        assertEquals(PayPalRecurringBillingPlanType.RECURRING, request.recurringBillingPlanType)
        assertEquals("USD", request.recurringBillingDetails?.currencyISOCode)
        assertEquals("2.00", request.recurringBillingDetails?.oneTimeFeeAmount)
        assertEquals("A Product", request.recurringBillingDetails?.productName)
        assertEquals("A Description", request.recurringBillingDetails?.productDescription)
        assertSame(1, requireNotNull(request.recurringBillingDetails?.productQuantity))
        assertEquals("5.00", request.recurringBillingDetails?.shippingAmount)
        assertEquals("3.00", request.recurringBillingDetails?.taxAmount)
        assertEquals("11.00", request.recurringBillingDetails?.totalAmount)
        val requestBillingCycle = request.recurringBillingDetails?.billingCycles?.get(0)
        assertEquals(PayPalBillingInterval.MONTH, requestBillingCycle?.interval)
        assertSame(1, requestBillingCycle?.intervalCount)
        assertEquals(2, requestBillingCycle?.numberOfExecutions)
        assertEquals("2024-04-06T00:00:00Z", requestBillingCycle?.startDate)
        assertSame(1, requestBillingCycle?.sequence)
        assertTrue(requestBillingCycle!!.isTrial)
        val requestBillingPricing = requestBillingCycle.pricing
        assertEquals("6.00", requestBillingPricing?.reloadThresholdAmount)
        assertEquals("1.00", requestBillingPricing?.amount)
        assertEquals(PayPalPricingModel.FIXED, requestBillingPricing?.pricingModel)
    }

    @Test
    @Suppress("LongMethod")
    fun `creates PayPalVaultRequest and parcels it correctly`() {
        val postalAddress = PostalAddress().apply {
            recipientName = "Postal Address"
        }
        val request = PayPalVaultRequest(true).apply {
            localeCode = "en-US"
            billingAgreementDescription = "Billing Agreement Description"
            isShippingAddressRequired = true
            isShippingAddressEditable = true
            shouldOfferCredit = true
            shippingAddressOverride = postalAddress
            displayName = "Display Name"
            riskCorrelationId = "123-correlation"
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
            merchantAccountId = "merchant_account_id"
            userPhoneNumber = PayPalPhoneNumber("1", "1231231234")
        }

        val billingInterval = PayPalBillingInterval.MONTH
        val pricingModel = PayPalPricingModel.FIXED
        val billingPricing = PayPalBillingPricing(pricingModel, "1.00").apply {
            reloadThresholdAmount = "6.00"
        }

        val billingCycle = PayPalBillingCycle(true, 2, billingInterval, 1)
            .apply {
                sequence = 1
                startDate = "2024-04-06T00:00:00Z"
                pricing = billingPricing
            }

        val billingDetails = PayPalRecurringBillingDetails(listOf(billingCycle), "11.00", "USD")
            .apply {
                oneTimeFeeAmount = "2.00"
                productName = "A Product"
                productDescription = "A Description"
                productQuantity = 1
                shippingAmount = "5.00"
                taxAmount = "3.00"
            }

        request.apply {
            recurringBillingDetails = billingDetails
            recurringBillingPlanType = PayPalRecurringBillingPlanType.RECURRING
        }

        val lineItems = ArrayList<PayPalLineItem>()
        lineItems.add(PayPalLineItem(PayPalLineItemKind.DEBIT, "An Item", "1", "1"))
        request.lineItems = lineItems

        val parcel = Parcel.obtain().apply {
            request.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val result = parcelableCreator<PayPalVaultRequest>().createFromParcel(parcel)

        assertEquals("en-US", result.localeCode)
        assertEquals("Billing Agreement Description", result.billingAgreementDescription)
        assertTrue(result.shouldOfferCredit)
        assertTrue(result.isShippingAddressRequired)
        assertTrue(result.isShippingAddressEditable)
        assertEquals("Postal Address", result.shippingAddressOverride?.recipientName)
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, result.landingPageType)
        assertEquals("Display Name", result.displayName)
        assertEquals("123-correlation", result.riskCorrelationId)
        assertEquals("merchant_account_id", result.merchantAccountId)
        assertEquals(1, result.lineItems.size)
        assertEquals("An Item", result.lineItems[0].name)
        assertEquals("1", result.userPhoneNumber?.countryCode)
        assertEquals("1231231234", result.userPhoneNumber?.nationalNumber)
        assertTrue(result.hasUserLocationConsent)
        assertEquals(PayPalRecurringBillingPlanType.RECURRING, result.recurringBillingPlanType)
        assertEquals("USD", result.recurringBillingDetails?.currencyISOCode)
        assertEquals("2.00", result.recurringBillingDetails?.oneTimeFeeAmount)
        assertEquals("A Product", result.recurringBillingDetails?.productName)
        assertEquals("A Description", result.recurringBillingDetails?.productDescription)
        assertSame(1, requireNotNull(result.recurringBillingDetails?.productQuantity))
        assertEquals("5.00", result.recurringBillingDetails?.shippingAmount)
        assertEquals("3.00", result.recurringBillingDetails?.taxAmount)
        assertEquals("11.00", result.recurringBillingDetails?.totalAmount)
        val resultBillingCycle = result.recurringBillingDetails?.billingCycles?.get(0)
        assertEquals(PayPalBillingInterval.MONTH, resultBillingCycle?.interval)
        assertSame(1, resultBillingCycle?.intervalCount)
        assertEquals(2, resultBillingCycle?.numberOfExecutions)
        assertEquals("2024-04-06T00:00:00Z", resultBillingCycle?.startDate)
        assertSame(1, resultBillingCycle?.sequence)
        assertTrue(resultBillingCycle!!.isTrial)
        val resultBillingPricing = resultBillingCycle.pricing
        assertEquals("6.00", resultBillingPricing?.reloadThresholdAmount)
        assertEquals("1.00", resultBillingPricing?.amount)
        assertEquals(PayPalPricingModel.FIXED, resultBillingPricing?.pricingModel)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates PayPaLVaultRequest and sets UserAuthenticationEmail when not null`() {
        val payerEmail = "payer_email@example.com"
        val request = PayPalVaultRequest(true).apply {
            userAuthenticationEmail = payerEmail
        }
        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"payer_email\":\"$payerEmail\""))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates PayPaLVaultRequest and sets UserAuthenticationEmail and EnablePayPalSwitch when not null`() {
        val versionSDK = Build.VERSION.SDK_INT.toString()
        val payerEmail = "payer_email@example.com"
        val request = PayPalVaultRequest(true).apply {
            enablePayPalAppSwitch = true
            userAuthenticationEmail = payerEmail
        }
        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = "universal_url"
        )

        assertTrue(requestBody.contains("\"launch_paypal_app\":true"))
        assertTrue(requestBody.contains("\"os_type\":" + "\"Android\""))
        assertTrue(requestBody.contains("\"os_version\":\"$versionSDK\""))
        assertTrue(requestBody.contains("\"merchant_app_return_url\":" + "\"universal_url\""))
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun `creates PayPaLVaultRequest and sets ShopperInsightsSessionId correctly`() {
        val request = PayPalVaultRequest(true).apply {
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
    fun `creates PayPalVaultRequest and formats JSON correctly`() {
        val postalAddress = PostalAddress().apply {
            recipientName = "Postal Address"
        }
        val request = PayPalVaultRequest(true).apply {
            localeCode = "en-US"
            billingAgreementDescription = "Billing Agreement Description"
            isShippingAddressRequired = true
            isShippingAddressEditable = true
            shouldOfferCredit = true
            userAuthenticationEmail = "email"
            shippingAddressOverride = postalAddress
            displayName = "Display Name"
            riskCorrelationId = "123-correlation"
            landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
            merchantAccountId = "merchant_account_id"
        }

        val billingInterval = PayPalBillingInterval.MONTH
        val pricingModel = PayPalPricingModel.VARIABLE
        val billingPricing = PayPalBillingPricing(pricingModel, "1.00").apply {
            reloadThresholdAmount = "6.00"
        }

        val billingCycle = PayPalBillingCycle(true, 2, billingInterval, 1)
            .apply {
                sequence = 1
                startDate = "2024-04-06T00:00:00Z"
                pricing = billingPricing
            }

        val billingDetails = PayPalRecurringBillingDetails(listOf(billingCycle), "11.00", "USD")
            .apply {
                oneTimeFeeAmount = "2.00"
                productName = "A Product"
                productDescription = "A Description"
                productQuantity = 1
                shippingAmount = "5.00"
                taxAmount = "3.00"
            }

        request.apply {
            recurringBillingDetails = billingDetails
            recurringBillingPlanType = PayPalRecurringBillingPlanType.RECURRING
        }

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )
        JSONAssert.assertEquals(Fixtures.PAYPAL_REQUEST_JSON, requestBody, false)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates RequestBody and sets appSwitchParameters correctly regardless of UserAuthenticationEmail value`(
        @TestParameter(value = ["", "some@email.com"]) payerEmail: String
    ) {
        val request = PayPalVaultRequest(true).apply {
            enablePayPalAppSwitch = true
            userAuthenticationEmail = payerEmail
        }

        val appLink = "universal_url"

        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink
        )

        val jsonObject = JSONObject(requestBody)
        assertTrue(jsonObject.getBoolean("launch_paypal_app"))
        assertEquals("Android", jsonObject.getString("os_type"))
        assertEquals(appLink, jsonObject.getString("merchant_app_return_url"))
        assertNotNull(jsonObject.getString("os_version"))
    }

    @Test
    @Throws(JSONException::class)
    fun `creates PayPaLVaultRequest and sets UserPhoneNumber when not null`() {
        val payerPhoneNumber = PayPalPhoneNumber("1", "1231231234")
        val request = PayPalVaultRequest(true).apply {
            userPhoneNumber = payerPhoneNumber
        }
        val requestBody = request.createRequestBody(
            configuration = mockk<Configuration>(relaxed = true),
            authorization = mockk<Authorization>(relaxed = true),
            successUrl = "success_url",
            cancelUrl = "cancel_url",
            appLink = null
        )

        assertTrue(requestBody.contains("\"phone_number\":{\"country_code\":\"1\",\"national_number\":\"1231231234\"}"))
    }
}
