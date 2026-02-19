package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.SetAppSwitchUseCase
import com.braintreepayments.api.core.TokenizationKey
import com.braintreepayments.api.core.usecase.GetAppSwitchUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult.DeepLink
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
import com.braintreepayments.api.paypal.PayPalAccountNonce.Companion.fromJSON
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalInternalClientUnitTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var configuration: Configuration

    private lateinit var clientToken: ClientToken
    private lateinit var tokenizationKey: TokenizationKey

    private lateinit var dataCollector: DataCollector
    private lateinit var apiClient: ApiClient
    private lateinit var deviceInspector: DeviceInspector

    private lateinit var payPalInternalClientCallback: PayPalInternalClientCallback

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val getReturnLinkUseCase: GetReturnLinkUseCase = mockk(relaxed = true)
    private val setAppSwitchUseCase: SetAppSwitchUseCase = mockk(relaxed = true)
    private val getAppSwitchUseCase: GetAppSwitchUseCase = mockk(relaxed = true)
    private val resolvePayPalUseCase: ResolvePayPalUseCase = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        context = mockk(relaxed = true)

        every { resolvePayPalUseCase() } returns false

        clientToken = mockk(relaxed = true)
        tokenizationKey = mockk(relaxed = true)
        configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)

        dataCollector = mockk(relaxed = true)
        apiClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        payPalInternalClientCallback = mockk(relaxed = true)

        every { getReturnLinkUseCase.invoke(any()) } returns ReturnLinkResult.AppLink(
            Uri.parse("https://example.com")
        )
    }

    private fun createShippingAddressOverride(): PostalAddress {
        val address = PostalAddress()
        address.recipientName = "Brianna Tree"
        address.streetAddress = "123 Fake St."
        address.extendedAddress = "Apt. v.0"
        address.locality = "Oakland"
        address.region = "CA"
        address.postalCode = "12345"
        address.countryCodeAlpha2 = "US"
        return address
    }

    private fun createPayPalVaultRequestWithShipping(): PayPalVaultRequest {
        val request = PayPalVaultRequest(true)
        request.billingAgreementDescription = "Billing Agreement Description"
        request.merchantAccountId = "sample-merchant-account-id"
        request.landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_BILLING
        request.displayName = "sample-display-name"
        request.localeCode = "US"
        request.isShippingAddressRequired = true
        request.isShippingAddressEditable = true
        request.shouldOfferCredit = true
        request.shippingAddressOverride = createShippingAddressOverride()
        return request
    }

    private fun createPayPalCheckoutRequestWithAllParams(
        shippingAddressOverride: PostalAddress,
        item: PayPalLineItem
    ): PayPalCheckoutRequest {
        val request = PayPalCheckoutRequest("1.00", true)
        request.currencyCode = "USD"
        request.intent = PayPalPaymentIntent.AUTHORIZE
        request.shouldRequestBillingAgreement = true
        request.billingAgreementDescription = "Billing Agreement Description"
        request.merchantAccountId = "sample-merchant-account-id"
        request.landingPageType = PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN
        request.displayName = "sample-display-name"
        request.localeCode = "US"
        request.isShippingAddressRequired = true
        request.isShippingAddressEditable = true
        request.shouldOfferPayLater = true
        request.shouldOfferCredit = true
        request.lineItems = listOf(item)
        request.shippingAddressOverride = shippingAddressOverride
        return request
    }

    private fun createPayPalLineItem(): PayPalLineItem {
        val item = PayPalLineItem(
            PayPalLineItemKind.DEBIT,
            "Item 0",
            "1",
            "2"
        )
        item.description = "A new item"
        item.productCode = "abc-123"
        item.unitTaxAmount = "1.50"
        item.url = "http://example.com"
        return item
    }

    private fun expectedPayPalCheckoutRequestJson(): JSONObject {
        return JSONObject(
            """
            {
                "amount": "1.00",
                "currency_iso_code": "USD",
                "intent": "authorize",
                "authorization_fingerprint": "client-token-bearer",
                "return_url": "https://example.com://onetouch/v1/success",
                "cancel_url": "https://example.com://onetouch/v1/cancel",
                "offer_pay_later": true,
                "offer_paypal_credit": true,
                "request_billing_agreement": true,
                "billing_agreement_details": {
                    "description": "Billing Agreement Description"
                },
                "line_items": [
                    {
                        "kind": "debit",
                        "name": "Item 0",
                        "quantity": "1",
                        "unit_amount": "2",
                        "description": "A new item",
                        "product_code": "abc-123",
                        "unit_tax_amount": "1.50",
                        "url": "http://example.com"
                    }
                ],
                "experience_profile": {
                    "no_shipping": false,
                    "landing_page_type": "login",
                    "brand_name": "sample-display-name",
                    "locale_code": "US",
                    "address_override": false
                },
                "line1": "123 Fake St.",
                "line2": "Apt. v.0",
                "city": "Oakland",
                "state": "CA",
                "postal_code": "12345",
                "country_code": "US",
                "recipient_name": "Brianna Tree",
                "merchant_account_id": "sample-merchant-account-id"
            }
            """.trimIndent()
        )
    }

    private fun createSutWithMocks(
        apiClient: ApiClient = this.apiClient,
        fixture: String? = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_BA_TOKEN_PARAM,
        error: Exception? = null,
        captureRequestBody: CapturingSlot<String>? = null
    ): PayPalInternalClient {
        val braintreeClient = if (error != null) {
            MockkBraintreeClientBuilder()
                .sendPostErrorResponse(error)
                .build()
        } else {
            MockkBraintreeClientBuilder()
                .sendPostSuccessfulResponse(fixture!!)
                .build()
        }

        captureRequestBody?.let { slot ->
            coEvery {
                braintreeClient.sendPOST(
                    url = any(),
                    data = capture(slot)
                )
            } returns fixture!!
        }

        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        val sut = PayPalInternalClient(
            braintreeClient,
            dataCollector,
            apiClient,
            deviceInspector,
            merchantRepository,
            getReturnLinkUseCase,
            setAppSwitchUseCase,
            getAppSwitchUseCase,
            resolvePayPalUseCase,
            analyticsParamRepository,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        return sut
    }

    private fun createCheckoutRequest(): PayPalCheckoutRequest {
        val request = PayPalCheckoutRequest("1.00", true)
        request.intent = PayPalPaymentIntent.AUTHORIZE
        request.merchantAccountId = "sample-merchant-account-id"
        request.userAction = PayPalPaymentUserAction.USER_ACTION_COMMIT
        request.riskCorrelationId = "sample-client-metadata-id"
        return request
    }

    private fun assertPayPalCheckoutParams(params: PayPalPaymentAuthRequestParams, expectedUrl: String) {
        assertFalse(params.isBillingAgreement)
        assertEquals(PayPalPaymentIntent.AUTHORIZE, params.intent)
        assertEquals("sample-merchant-account-id", params.merchantAccountId)
        assertEquals("https://example.com://onetouch/v1/success", params.successUrl)
        assertEquals("fake-token", params.contextId)
        assertEquals("sample-client-metadata-id", params.clientMetadataId)
        assertEquals(expectedUrl, params.approvalUrl)
    }

    private fun createVaultRequest(): PayPalVaultRequest {
        val request = PayPalVaultRequest(true)
        request.merchantAccountId = "sample-merchant-account-id"
        request.riskCorrelationId = "sample-client-metadata-id"
        return request
    }

    private fun assertPayPalVaultParams(params: PayPalPaymentAuthRequestParams, expectedUrl: String) {
        assertTrue(params.isBillingAgreement)
        assertEquals("sample-merchant-account-id", params.merchantAccountId)
        assertEquals("https://example.com://onetouch/v1/success", params.successUrl)
        assertEquals("fake-ba-token", params.contextId)
        assertEquals("sample-client-metadata-id", params.clientMetadataId)
        assertEquals(expectedUrl, params.approvalUrl)
    }

    @Test
    fun sendRequest_withPayPalVaultRequest_sendsAllParameters() = runTest(testDispatcher) {

        every { clientToken.bearer } returns "client-token-bearer"
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = createPayPalVaultRequestWithShipping()

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        val expected = JSONObject()
            .put("authorization_fingerprint", "client-token-bearer")
            .put("return_url", "https://example.com://onetouch/v1/success")
            .put("cancel_url", "https://example.com://onetouch/v1/cancel")
            .put("offer_paypal_credit", true)
            .put("description", "Billing Agreement Description")
            .put(
                "experience_profile", JSONObject()
                    .put("no_shipping", false)
                    .put("landing_page_type", "billing")
                    .put("brand_name", "sample-display-name")
                    .put("locale_code", "US")
                    .put("address_override", false)
            )
            .put(
                "shipping_address", JSONObject()
                    .put("line1", "123 Fake St.")
                    .put("line2", "Apt. v.0")
                    .put("city", "Oakland")
                    .put("state", "CA")
                    .put("postal_code", "12345")
                    .put("country_code", "US")
                    .put("recipient_name", "Brianna Tree")
            )
            .put("merchant_account_id", "sample-merchant-account-id")

        JSONAssert.assertEquals(expected, actual, true)
    }

    @Test
    fun sendRequest_withPayPalVaultRequest_sendsAllParameters_with_deep_link() = runTest(testDispatcher) {
        every { getReturnLinkUseCase.invoke() } returns DeepLink("com.braintreepayments.demo")

        every { clientToken.bearer } returns "client-token-bearer"
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.returnUrlScheme } returns "com.braintreepayments.demo"

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = createPayPalVaultRequestWithShipping()

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        val expected = JSONObject()
            .put("authorization_fingerprint", "client-token-bearer")
            .put("return_url", "com.braintreepayments.demo://onetouch/v1/success")
            .put("cancel_url", "com.braintreepayments.demo://onetouch/v1/cancel")
            .put("offer_paypal_credit", true)
            .put("description", "Billing Agreement Description")
            .put(
                "experience_profile", JSONObject()
                    .put("no_shipping", false)
                    .put("landing_page_type", "billing")
                    .put("brand_name", "sample-display-name")
                    .put("locale_code", "US")
                    .put("address_override", false)
            )
            .put(
                "shipping_address", JSONObject()
                    .put("line1", "123 Fake St.")
                    .put("line2", "Apt. v.0")
                    .put("city", "Oakland")
                    .put("state", "CA")
                    .put("postal_code", "12345")
                    .put("country_code", "US")
                    .put("recipient_name", "Brianna Tree")
            )
            .put("merchant_account_id", "sample-merchant-account-id")

        JSONAssert.assertEquals(expected, actual, true)
    }

    @Test
    fun sendRequest_withPayPalCheckoutRequest_sendsAllParameters() = runTest(testDispatcher) {
        every { clientToken.bearer } returns "client-token-bearer"
        every { merchantRepository.authorization } returns clientToken

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)
        val shippingAddressOverride = createShippingAddressOverride()
        val item = createPayPalLineItem()
        val payPalRequest = createPayPalCheckoutRequestWithAllParams(shippingAddressOverride, item)

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val actual = JSONObject(slot.captured)
        val expected = expectedPayPalCheckoutRequestJson()
        JSONAssert.assertEquals(expected, actual, true)
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withTokenizationKey_sendsClientKeyParam() = runTest(testDispatcher) {
        every { tokenizationKey.bearer } returns "tokenization-key-bearer"

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        every { merchantRepository.authorization } returns tokenizationKey

        val payPalRequest = PayPalVaultRequest(true)
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertFalse(actual.has("authorization_fingerprint"))
        assertEquals("tokenization-key-bearer", actual["client_key"])
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withEmptyDisplayName_fallsBackToPayPalConfigurationDisplayName() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(false)
        payPalRequest.displayName = ""
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertEquals(
            "paypal_merchant",
            (actual["experience_profile"] as JSONObject)["brand_name"]
        )
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withLocaleNotSpecified_omitsLocale() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.localeCode = null
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)
        assertFalse((actual["experience_profile"] as JSONObject).has("locale_code"))
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withMerchantAccountIdNotSpecified_omitsMerchantAccountId() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.merchantAccountId = null
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertFalse(actual.has("merchant_account_id"))
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withShippingAddressOverrideNotSpecified_sendsAddressOverrideFalse() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.shippingAddressOverride = null
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertEquals(
            false,
            (actual["experience_profile"] as JSONObject)["address_override"]
        )
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withShippingAddressSpecified_sendsAddressOverrideBasedOnShippingAddressEditability() =
        runTest(testDispatcher) {
        every { clientToken.bearer } returns "client-token-bearer"
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.isShippingAddressEditable = false
        payPalRequest.shippingAddressOverride = PostalAddress()

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertEquals(
            true,
            (actual["experience_profile"] as JSONObject)["address_override"]
        )
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withPayPalVaultRequest_omitsEmptyBillingAgreementDescription() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.billingAgreementDescription = ""
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertFalse(actual.has("description"))
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withPayPalCheckoutRequest_fallsBackToPayPalConfigurationCurrencyCode() = runTest(testDispatcher) {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_INR)
        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)

        assertEquals("INR", actual["currency_iso_code"])
    }

    @Test
    @Throws(JSONException::class)
    fun sendRequest_withPayPalCheckoutRequest_omitsEmptyLineItems() = runTest(testDispatcher) {

        every { merchantRepository.authorization } returns tokenizationKey

        val slot = slot<String>()
        val sut = createSutWithMocks(captureRequestBody = slot)

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.lineItems = ArrayList()
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val result = slot.captured
        val actual = JSONObject(result)
        assertFalse(actual.has("line_items"))
    }

    @Test
    fun sendRequest_whenRiskCorrelationIdNotNull_setsClientMetadataIdToRiskCorrelationId() = runTest(testDispatcher) {
        every {
            dataCollector.getClientMetadataId(
                eq(context),
                eq(configuration),
                eq(true)
            )
        } returns "sample-client-metadata-id"

        every { merchantRepository.authorization } returns clientToken

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.riskCorrelationId = "risk-correlation-id"

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify {
            payPalInternalClientCallback.onResult(capture(slot), null)
        }
        val payPalPaymentAuthRequestParams = slot.captured
        assertEquals("risk-correlation-id", payPalPaymentAuthRequestParams.clientMetadataId)
    }

    @Test
    fun sendRequest_whenRiskCorrelationIdNull_setsClientMetadataIdFromPayPalDataCollector() = runTest(testDispatcher) {
        every {
            dataCollector.getClientMetadataId(
                eq(context),
                any(),
                eq(configuration)
            )
        } returns "sample-client-metadata-id"

        every { merchantRepository.authorization } returns clientToken
        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify {
            payPalInternalClientCallback.onResult(capture(slot), null)
        }
        val payPalPaymentAuthRequestParams = slot.captured
        assertNull(payPalRequest.riskCorrelationId)
        assertEquals(
            "sample-client-metadata-id",
            payPalPaymentAuthRequestParams.clientMetadataId
        )
    }

    @Test
    fun sendRequest_withPayPalVaultRequest_callsBackPayPalResponseOnSuccess() = runTest(testDispatcher) {
        every {
            dataCollector.getClientMetadataId(
                eq(context),
                eq(configuration),
                eq(true)
            )
        } returns "sample-client-metadata-id"

        val sut = createSutWithMocks()
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")

        val payPalRequest = createVaultRequest()
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val expectedUrl = "https://checkout.paypal.com/one-touch-login-sandbox/index.html?" +
            "ba_token=fake-ba-token&action=create_payment_resource&amount=1.00&" +
            "authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%" +
            "7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%" +
            "3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8&" +
            "cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fcancel&" +
            "controller=client_api%2Fpaypal_hermes&currency_iso_code=USD&" +
            "experience_profile%5Baddress_override%5D=false&" +
            "experience_profile%5Bno_shipping%5D=false&" +
            "merchant_id=dcpspy2brwdjr3qn&" +
            "return_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess&" +
            "offer_paypal_credit=true&version=1"
        assertPayPalVaultParams(slot.captured, expectedUrl)
    }

    @Test
    fun sendRequest_whenServerReturnsNonAppSwitchFlow_setsDidPayPalServerAttemptAppSwitchToFalse() =
        runTest(testDispatcher) {
        every {
            dataCollector.getClientMetadataId(
                eq(context),
                eq(configuration),
                eq(true)
            )
        } returns "sample-client-metadata-id"

        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")

        val sut = createSutWithMocks()

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.merchantAccountId = "sample-merchant-account-id"
        payPalRequest.riskCorrelationId = "sample-client-metadata-id"

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify { analyticsParamRepository.didPayPalServerAttemptAppSwitch = false }
    }

    @Test
    fun sendRequest_withPayPalVaultRequest_callsBackPayPalResponseOnSuccess_returnsPayPalURL() =
        runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { getAppSwitchUseCase.invoke() } returns true
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_PAYPAL_REDIRECT_URL
        )

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.userAuthenticationEmail = "example@mail.com"
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = true,
                appSwitchFlowFromPayPalResponse = true
            )
        }

        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }
        val payPalPaymentAuthRequestParams = slot.captured
        assertTrue(payPalPaymentAuthRequestParams.isBillingAgreement)

        val approvalUri = Uri.parse(payPalPaymentAuthRequestParams.approvalUrl)
        val contextId = approvalUri.getQueryParameter("ba_token")
        assertNotNull(contextId)
        assertEquals(contextId, payPalPaymentAuthRequestParams.contextId)
        assertNotNull(approvalUri.getQueryParameter("source"))
        assertNotNull(approvalUri.getQueryParameter("switch_initiated_time"))
        assertEquals(approvalUri.host, "paypal.com")
    }

    @Test
    fun sendRequest_withPayPalVaultRequest_callsBackPayPalResponseOnSuccess_returnsApprovalURL() =
        runTest(testDispatcher) {

        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_APPROVAL_URL
        )

        val payPalRequest = PayPalVaultRequest(true)

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify {
            payPalInternalClientCallback.onResult(capture(slot), null)
        }

        val expectedUrl = "https://www.example.com/some?ba_token=fake-ba-token"
        val payPalPaymentAuthRequestParams = slot.captured
        assertTrue(payPalPaymentAuthRequestParams.isBillingAgreement)
        assertEquals("fake-ba-token", payPalPaymentAuthRequestParams.contextId)
        assertEquals(expectedUrl, payPalPaymentAuthRequestParams.approvalUrl)
    }

    @Test
    fun sendRequest_withPayPalCheckoutRequest_callsBackPayPalResponseOnSuccess() = runTest(testDispatcher) {
        every {
            dataCollector.getClientMetadataId(eq(context), eq(configuration), eq(true))
        } returns "sample-client-metadata-id"

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_TOKEN_PARAM
        )
        val payPalRequest = createCheckoutRequest()
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val expectedUrl =
            "https://checkout.paypal.com/one-touch-login-sandbox/index.html?" +
                "token=fake-token&action=create_payment_resource&amount=1.00&" +
                "authorization_fingerprint=63cc461306c35080ce674a3372bffe1580b4130c7fd33d33968aa76bb93cdd06%" +
                "7Ccreated_at%3D2015-10-13T18%3A49%3A48.371382792%2B0000%26merchant_id%" +
                "3Ddcpspy2brwdjr3qn%26public_key%3D9wwrzqk3vr3t4nc8&" +
                "cancel_url=com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%" +
                "2Fv1%2Fcancel&controller=client_api%2Fpaypal_hermes&currency_iso_code=USD&" +
                "experience_profile%5Baddress_override%5D=false&experience_profile%5" +
                "Bno_shipping%5D=false&merchant_id=dcpspy2brwdjr3qn&return_url=" +
                "com.braintreepayments.api.test.braintree%3A%2F%2Fonetouch%2Fv1%2Fsuccess&" +
                "offer_paypal_credit=true&version=1"
        assertPayPalCheckoutParams(slot.captured, expectedUrl)
    }

    @Test
    fun sendRequest_propagatesHttpErrors() = runTest(testDispatcher) {
        val httpError = IOException("http error")
        every { getReturnLinkUseCase.invoke() } returns ReturnLinkResult.AppLink(Uri.parse("https://example.com"))
        every { merchantRepository.authorization } returns clientToken
        val sut = createSutWithMocks(error = httpError)

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify { payPalInternalClientCallback.onResult(null, httpError) }
    }

    @Test
    fun sendRequest_propagatesMalformedJSONResponseErrors() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        val sut = createSutWithMocks(fixture = "{bad:")

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify {
            payPalInternalClientCallback.onResult(
                null,
                ofType(JSONException::class)
            )
        }
    }

    @Test
    fun sendRequest_returnLinkResultFailure_forwardsError() {
        val exception = BraintreeException()
        every { getReturnLinkUseCase.invoke() } returns ReturnLinkResult.Failure(exception)
        every { merchantRepository.authorization } returns clientToken

        val sut = createSutWithMocks(fixture = "{bad:")

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)

        verify { payPalInternalClientCallback.onResult(null, eq(exception)) }
    }

    @Test
    fun tokenize_tokenizesWithApiClient() = runTest(testDispatcher) {
        val payPalAccount = mockk<PayPalAccount>(relaxed = true)
        val callback = mockk<PayPalInternalTokenizeCallback>(relaxed = true)
        val apiClient = mockk<ApiClient>(relaxed = true)

        val sut = createSutWithMocks(apiClient = apiClient)

        sut.tokenize(payPalAccount, callback)
        advanceUntilIdle()

        coVerify {
            apiClient.tokenizeREST(eq(payPalAccount))
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_onTokenizeResult_returnsAccountNonceToCallback() = runTest(testDispatcher) {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(
                JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
            )
            .build()
        val payPalAccount = mockk<PayPalAccount>(relaxed = true)
        val callback = mockk<PayPalInternalTokenizeCallback>(relaxed = true)

        val sut = createSutWithMocks(apiClient = apiClient)

        sut.tokenize(payPalAccount, callback)
        advanceUntilIdle()

        val slot = slot<PayPalAccountNonce>()
        coVerify {
            callback.onResult(capture(slot), null)
        }

        val expectedNonce = fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        )
        val result = slot.captured
        assertEquals(expectedNonce.string, result.string)
    }

    @Test
    fun tokenize_onTokenizeError_returnsErrorToCallback() = runTest(testDispatcher) {
        val error = IOException("error")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()
        val payPalAccount = mockk<PayPalAccount>(relaxed = true)
        val callback = mockk<PayPalInternalTokenizeCallback>(relaxed = true)

        val sut = createSutWithMocks(apiClient = apiClient)

        sut.tokenize(payPalAccount, callback)
        advanceUntilIdle()

        coVerify {
            callback.onResult(null, error)
        }
    }

    @Test
    @Throws(Exception::class)
    fun payPalDataCollector_passes_correct_arguments_to_getClientMetadataId() = runTest(testDispatcher) {
        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)

        every { merchantRepository.authorization } returns clientToken

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.intent = PayPalPaymentIntent.AUTHORIZE
        payPalRequest.merchantAccountId = "sample-merchant-account-id"

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<DataCollectorInternalRequest>()
        verify {
            dataCollector.getClientMetadataId(
                context,
                capture(slot),
                configuration
            )
        }

        assertTrue(slot.captured.hasUserLocationConsent)
    }

    @Test
    fun sendRequest_withPayPalVaultRequestAndAppSwitchEnabled_addsAppSwitchParameters() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { getAppSwitchUseCase.invoke() } returns true
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_PAYPAL_REDIRECT_URL
        )

        val payPalRequest = PayPalVaultRequest(true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val approvalUri = Uri.parse(slot.captured.approvalUrl)
        assertEquals("braintree_sdk", approvalUri.getQueryParameter("source"))
        assertEquals("va", approvalUri.getQueryParameter("flow_type"))
        assertEquals(configuration.merchantId, approvalUri.getQueryParameter("merchant"))
        assertNotNull(approvalUri.getQueryParameter("switch_initiated_time"))
        assertTrue(approvalUri.getQueryParameter("switch_initiated_time")!!.toLong() > 0)
    }

    @Test
    fun sendRequest_withPayPalCheckoutRequestAndAppSwitchEnabled_addsAppSwitchParameters() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { getAppSwitchUseCase.invoke() } returns true
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_TOKEN_PARAM
        )

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val approvalUri = Uri.parse(slot.captured.approvalUrl)
        assertEquals("braintree_sdk", approvalUri.getQueryParameter("source"))
        assertEquals("ecs", approvalUri.getQueryParameter("flow_type"))
        assertEquals(PayPalFundingSource.PAYPAL.value, approvalUri.getQueryParameter("funding_source"))
        assertEquals(configuration.merchantId, approvalUri.getQueryParameter("merchant"))
        assertNotNull(approvalUri.getQueryParameter("switch_initiated_time"))
        assertTrue(approvalUri.getQueryParameter("switch_initiated_time")!!.toLong() > 0)
    }

    @Test
    fun sendRequest_whenShouldOfferCredit_addsCreditQueryParameter() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { getAppSwitchUseCase.invoke() } returns true
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_TOKEN_PARAM
        )

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true
        payPalRequest.shouldOfferCredit = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val approvalUri = Uri.parse(slot.captured.approvalUrl)
        assertEquals(PayPalFundingSource.CREDIT.value, approvalUri.getQueryParameter("funding_source"))
    }

    @Test
    fun sendRequest_whenShouldOfferPayLater_addsPayLaterQueryParameter() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { getAppSwitchUseCase.invoke() } returns true
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_TOKEN_PARAM
        )

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true
        payPalRequest.shouldOfferPayLater = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        val slot = slot<PayPalPaymentAuthRequestParams>()
        verify { payPalInternalClientCallback.onResult(capture(slot), null) }

        val approvalUri = Uri.parse(slot.captured.approvalUrl)
        assertEquals(PayPalFundingSource.PAY_LATER.value, approvalUri.getQueryParameter("funding_source"))
    }

    @Test
    fun sendRequest_whenPayPalAppNotInstalled_disablesAppSwitch() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { deviceInspector.isPayPalInstalled() } returns false
        every { resolvePayPalUseCase() } returns false

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = false,
                appSwitchFlowFromPayPalResponse = false
            )
        }
    }

    @Test
    fun sendRequest_whenPayPalAppCannotHandleURL_disablesAppSwitch() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns false

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = false,
                appSwitchFlowFromPayPalResponse = false
            )
        }
    }

    @Test
    fun sendRequest_whenPayPalInstalledAndCanHandleURL_enablesAppSwitch() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = true,
                appSwitchFlowFromPayPalResponse = false
            )
        }
    }

    @Test
    fun sendRequest_whenEnablePayPalAppSwitchFalse_doesNotCheckPayPalInstallation() = runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")

        val sut = createSutWithMocks()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = false

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify(exactly = 0) { deviceInspector.isPayPalInstalled() }
        verify(exactly = 0) { resolvePayPalUseCase() }
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = false,
                appSwitchFlowFromPayPalResponse = false
            )
        }
    }

    @Test
    fun sendRequest_whenServerReturnsAppSwitchFlow_setsDidPayPalServerAttemptAppSwitchToTrue() =
        runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_PAYPAL_REDIRECT_URL
        )

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify { analyticsParamRepository.didPayPalServerAttemptAppSwitch = true }
        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = true,
                appSwitchFlowFromPayPalResponse = true
            )
        }
    }

    @Test
    fun sendRequest_whenMerchantEnablesAppSwitchButServerReturnsAppSwitchFalse_setsBothCorrectly() =
        runTest(testDispatcher) {
        every { merchantRepository.authorization } returns clientToken
        every { merchantRepository.appLinkReturnUri } returns Uri.parse("https://example.com")
        every { deviceInspector.isPayPalInstalled() } returns true
        every { resolvePayPalUseCase() } returns true

        val sut = createSutWithMocks(
            fixture = Fixtures.PAYPAL_HERMES_RESPONSE_WITH_BA_TOKEN_PARAM
        )

        val payPalRequest = PayPalCheckoutRequest("1.00", true)
        payPalRequest.enablePayPalAppSwitch = true

        sut.sendRequest(context, payPalRequest, configuration, payPalInternalClientCallback)
        advanceUntilIdle()
        verify { analyticsParamRepository.didPayPalServerAttemptAppSwitch = false }

        verify {
            setAppSwitchUseCase.invoke(
                merchantEnabledAppSwitch = true,
                appSwitchFlowFromPayPalResponse = false
            )
        }
    }
}
