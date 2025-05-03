package com.braintreepayments.api.googlepay

import android.content.pm.ActivityInfo
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.WalletConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.json.JSONObject
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SuppressWarnings("LongMethod")
@RunWith(RobolectricTestRunner::class)
class GooglePayClientUnitTest {

    private lateinit var activity: FragmentActivity
    private lateinit var baseRequest: GooglePayRequest
    private lateinit var readyToPayCallback: GooglePayIsReadyToPayCallback
    private lateinit var intentDataCallback: GooglePayPaymentAuthRequestCallback
    private lateinit var activityResultCallback: GooglePayTokenizeCallback
    private lateinit var activityInfo: ActivityInfo
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var merchantRepository: MerchantRepository

    @Before
    fun beforeEach() {
        activity = mockk(relaxed = true)
        readyToPayCallback = mockk(relaxed = true)
        activityResultCallback = mockk(relaxed = true)
        intentDataCallback = mockk(relaxed = true)
        activityInfo = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        merchantRepository = mockk(relaxed = true)

        baseRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        every { activityInfo.themeResource } returns R.style.bt_transparent_activity
    }

    @Test
    fun isReadyToPay_sendsReadyToPayRequest() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .supportedNetworks(arrayOf("amex", "visa"))
                    .enabled(true)
            )
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.isReadyToPay(activity, null, readyToPayCallback)

        val captor = slot<IsReadyToPayRequest>()
        verify {
            internalGooglePayClient.isReadyToPay(
                activity,
                configuration,
                capture(captor),
                any<GooglePayIsReadyToPayCallback>()
            )
        }

        val actualJson = captor.captured.toJson()
        JSONAssert.assertEquals(
            Fixtures.READY_TO_PAY_REQUEST_WITHOUT_EXISTING_PAYMENT_METHOD,
            actualJson,
            false
        )
    }

    @Test
    fun isReadyToPay_whenExistingPaymentMethodRequired_sendsIsReadyToPayRequestWithExistingPaymentRequired() {
        val readyForGooglePayRequest = ReadyForGooglePayRequest().apply {
            isExistingPaymentMethodRequired = true
        }

        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .supportedNetworks(arrayOf("amex", "visa"))
                    .enabled(true)
            )
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.isReadyToPay(activity, readyForGooglePayRequest, readyToPayCallback)

        val captor = slot<IsReadyToPayRequest>()
        verify {
            internalGooglePayClient.isReadyToPay(
                activity,
                configuration,
                capture(captor),
                any<GooglePayIsReadyToPayCallback>()
            )
        }

        val actualJson = captor.captured.toJson()
        JSONAssert.assertEquals(
            Fixtures.READY_TO_PAY_REQUEST_WITH_EXISTING_PAYMENT_METHOD,
            actualJson,
            false
        )
    }

    @Test
    fun isReadyToPay_returnsFalseWhenGooglePayIsNotEnabled() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .enabled(false)
            )
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.TOKENIZATION_KEY)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder()
            .isReadyToPay(true)
            .build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        sut.isReadyToPay(activity, null, readyToPayCallback)

        verify { readyToPayCallback.onGooglePayReadinessResult(ofType<GooglePayReadinessResult.NotReadyToPay>()) }
    }

    @Test
    fun createPaymentAuthRequest_resetsSessionId() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .enabled(false)
            )
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.TOKENIZATION_KEY)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder()
            .isReadyToPay(true)
            .build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val googlePayRequest = mockk<GooglePayRequest>(relaxed = true)
        val requestCallback = mockk<GooglePayPaymentAuthRequestCallback>(relaxed = true)
        sut.createPaymentAuthRequest(googlePayRequest, requestCallback)

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun createPaymentAuthRequest_callsBackIntentData() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every {
            merchantRepository.authorization
        } returns
                Authorization.fromString("sandbox_tokenization_string")
        every { merchantRepository.integrationType } returns IntegrationType.CUSTOM

        val googlePayRequest =
            GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL).apply {
                allowPrepaidCards = true
                billingAddressFormat = GooglePayBillingAddressFormat.FULL
                isBillingAddressRequired = true
                isEmailRequired = true
                isPhoneNumberRequired = true
                isShippingAddressRequired = true
                shippingAddressParameters = GooglePayShippingAddressParameters(listOf("USA"))
        }

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams
        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.googlePayEnvironment)
        val paymentDataRequest = intent.paymentDataRequest
        val paymentDataRequestJson = JSONObject(paymentDataRequest.toJson())

        assertEquals(2, paymentDataRequestJson.get("apiVersion"))
        assertEquals(0, paymentDataRequestJson.get("apiVersionMinor"))

        assertEquals(true, paymentDataRequestJson.get("emailRequired"))
        assertEquals(true, paymentDataRequestJson.get("shippingAddressRequired"))

        val transactionInfoJson = paymentDataRequestJson.getJSONObject("transactionInfo")
        assertEquals("FINAL", transactionInfoJson.getString("totalPriceStatus"))
        assertEquals("1.00", transactionInfoJson.getString("totalPrice"))
        assertEquals("USD", transactionInfoJson.getString("currencyCode"))

        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
        val paypal = allowedPaymentMethods.getJSONObject(0)
        assertEquals("PAYPAL", paypal.getString("type"))

        val purchaseUnits = paypal.getJSONObject("parameters")
            .getJSONObject("purchase_context")
            .getJSONArray("purchase_units")
        assertEquals(1, purchaseUnits.length())

        val purchaseUnit = purchaseUnits.getJSONObject(0)
        assertEquals("paypal-client-id-for-google-payment", purchaseUnit.getJSONObject("payee").getString("client_id"))
        assertEquals("true", purchaseUnit.getString("recurring_payment"))

        val paypalTokenizationSpecification = paypal.getJSONObject("tokenizationSpecification")
        assertEquals("PAYMENT_GATEWAY", paypalTokenizationSpecification.getString("type"))

        val paypalTokenizationSpecificationParams = paypalTokenizationSpecification.getJSONObject("parameters")
        assertEquals("braintree", paypalTokenizationSpecificationParams.getString("gateway"))
        assertEquals("v1", paypalTokenizationSpecificationParams.getString("braintree:apiVersion"))

        val googlePayModuleVersion = BuildConfig.VERSION_NAME
        assertEquals(googlePayModuleVersion, paypalTokenizationSpecificationParams.getString("braintree:sdkVersion"))
        assertEquals(
            "integration_merchant_id",
            paypalTokenizationSpecificationParams.getString("braintree:merchantId")
        )
        assertEquals(
            "{\"source\":\"client\",\"integration\":\"CUSTOM\"," +
                    "\"sessionId\":\"\",\"version\":\"$googlePayModuleVersion\",\"platform\":\"android\"}",
            paypalTokenizationSpecificationParams.getString("braintree:metadata")
        )
        assertFalse(paypalTokenizationSpecificationParams.has("braintree:clientKey"))
        assertEquals(
            "paypal-client-id-for-google-payment",
            paypalTokenizationSpecificationParams.getString("braintree:paypalClientId")
        )

        val card = allowedPaymentMethods.getJSONObject(1)
        assertEquals("CARD", card.getString("type"))

        val cardParams = card.getJSONObject("parameters")
        assertTrue(cardParams.getBoolean("billingAddressRequired"))
        assertTrue(cardParams.getBoolean("allowPrepaidCards"))

        assertEquals("PAN_ONLY", cardParams.getJSONArray("allowedAuthMethods").getString(0))
        assertEquals("CRYPTOGRAM_3DS", cardParams.getJSONArray("allowedAuthMethods").getString(1))

        assertEquals("VISA", cardParams.getJSONArray("allowedCardNetworks").getString(0))
        assertEquals("MASTERCARD", cardParams.getJSONArray("allowedCardNetworks").getString(1))
        assertEquals("AMEX", cardParams.getJSONArray("allowedCardNetworks").getString(2))
        assertEquals("DISCOVER", cardParams.getJSONArray("allowedCardNetworks").getString(3))

        val tokenizationSpecification = card.getJSONObject("tokenizationSpecification")
        assertEquals("PAYMENT_GATEWAY", tokenizationSpecification.getString("type"))

        val cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters")
        assertEquals("braintree", cardTokenizationSpecificationParams.getString("gateway"))
        assertEquals("v1", cardTokenizationSpecificationParams.getString("braintree:apiVersion"))
        assertEquals(googlePayModuleVersion, cardTokenizationSpecificationParams.getString("braintree:sdkVersion"))
        assertEquals("integration_merchant_id", cardTokenizationSpecificationParams.getString("braintree:merchantId"))
        assertEquals(
            "{\"source\":\"client\",\"integration\":\"CUSTOM\"," +
                    "\"sessionId\":\"\",\"version\":\"$googlePayModuleVersion\",\"platform\":\"android\"}",
            cardTokenizationSpecificationParams.getString("braintree:metadata")
        )
        assertEquals(
            "sandbox_tokenization_string",
            cardTokenizationSpecificationParams.getString("braintree:clientKey")
        )
    }

    @Test
    fun createPaymentAuthRequest_includesATokenizationKeyWhenPresent() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.TOKENIZATION_KEY)

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequest = intent.paymentDataRequest
        val paymentDataRequestJson = JSONObject(paymentDataRequest.toJson())

        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
        val card = allowedPaymentMethods.getJSONObject(0)
        val tokenizationSpecification = card.getJSONObject("tokenizationSpecification")
        val cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters")
        assertEquals(Fixtures.TOKENIZATION_KEY, cardTokenizationSpecificationParams.get("braintree:clientKey"))
    }

    @Test
    fun createPaymentAuthRequest_doesNotIncludeATokenizationKeyWhenNotPresent() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequest = intent.paymentDataRequest
        val paymentDataRequestJson = JSONObject(paymentDataRequest.toJson())

        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
        val card = allowedPaymentMethods.getJSONObject(0)
        val tokenizationSpecification = card.getJSONObject("tokenizationSpecification")
        val cardTokenizationSpecificationParams = tokenizationSpecification.getJSONObject("parameters")
        assertFalse(cardTokenizationSpecificationParams.has("braintree:clientKey"))
    }

    @Test
    fun createPaymentAuthRequest_sendsAnalyticsEvent() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(googlePayRequest, intentDataCallback)

        verifyOrder {
            braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_STARTED)
            braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED)
        }
    }

    @Test
    fun createPaymentAuthRequest_whenMerchantNotConfigured_returnsExceptionToFragment() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder().build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.Failure)
        val exception = request.error

        assertTrue(exception is BraintreeException)
        val expectedMessage =
            "Google Pay is not enabled for your Braintree account, " +
                    "or Google Play Services are not configured correctly."
        assertEquals(expectedMessage, exception.message)

        val params = AnalyticsEventParams(
            errorDescription = expectedMessage
        )
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_FAILED, params, true) }
    }

    @Test
    fun createPaymentAuthRequest_whenSandbox_setsTestEnvironment() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequest = intent.paymentDataRequest
        val paymentDataRequestJson = JSONObject(paymentDataRequest.toJson())

        assertEquals(WalletConstants.ENVIRONMENT_TEST, intent.googlePayEnvironment)
        assertEquals("TEST", paymentDataRequestJson.getString("environment"))
    }

    @Test
    fun createPaymentAuthRequest_whenProduction_setsProductionEnvironment() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("production")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequest = intent.paymentDataRequest
        val paymentDataRequestJson = JSONObject(paymentDataRequest.toJson())

        assertEquals(WalletConstants.ENVIRONMENT_PRODUCTION, intent.googlePayEnvironment)
        assertEquals("PRODUCTION", paymentDataRequestJson.getString("environment"))
    }

    @Test
    fun createPaymentAuthRequest_withGoogleMerchantName_sendGoogleMerchantName() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        assertEquals("google-merchant-name-override", paymentDataRequestJson
            .getJSONObject("merchantInfo")
            .getString("merchantName"))
    }

    @Test
    fun createPaymentAuthRequest_whenGooglePayCanProcessPayPal_tokenizationPropertiesIncludePayPal() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")

        assertEquals(2, allowedPaymentMethods.length())
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0).getString("type"))
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1).getString("type"))
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalDisabledByRequest_tokenizationPropertiesLackPayPal() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"
        baseRequest.isPayPalEnabled = false

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")

        assertEquals(1, allowedPaymentMethods.length())
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0).getString("type"))
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalDisabledAndGooglePayHasPayPalClientId_tokenizationPropsContainPayPal() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .paypalEnabled(false)
            .paypal(TestConfigurationBuilder.TestPayPalConfigurationBuilder(false))
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")

        assertEquals(2, allowedPaymentMethods.length())
        assertEquals("PAYPAL", allowedPaymentMethods.getJSONObject(0).getString("type"))
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(1).getString("type"))
    }

    @Test
    fun createPaymentAuthRequest_usesGooglePayConfigurationClientId() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .paypal(
                TestConfigurationBuilder.TestPayPalConfigurationBuilder(true)
                    .clientId("paypal-client-id-for-paypal")
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
        val paypal = allowedPaymentMethods.getJSONObject(0)

        assertEquals(
            "paypal-client-id-for-google-payment",
            paypal.getJSONObject("parameters")
                .getJSONObject("purchase_context")
                .getJSONArray("purchase_units")
                .getJSONObject(0)
                .getJSONObject("payee")
                .getString("client_id")
        )

        assertEquals(
            "paypal-client-id-for-google-payment",
            paypal.getJSONObject("tokenizationSpecification")
                .getJSONObject("parameters")
                .getString("braintree:paypalClientId")
        )
    }

    @Test
    fun createPaymentAuthRequest_whenGooglePayConfigurationLacksClientId_tokenizationPropertiesLackPayPal() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")

        assertEquals(1, allowedPaymentMethods.length())
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0).getString("type"))
        assertFalse(allowedPaymentMethods.toString().contains("paypal-client-id-for-google-payment"))
    }

    @Test
    fun createPaymentAuthRequest_whenConfigurationContainsElo_addsEloAndEloDebitToAllowedPaymentMethods() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("elo"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        baseRequest.googleMerchantName = "google-merchant-name-override"

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.ReadyToLaunch)
        val intent = request.requestParams

        val paymentDataRequestJson = JSONObject(intent.paymentDataRequest.toJson())
        val allowedCardNetworks = paymentDataRequestJson
            .getJSONArray("allowedPaymentMethods")
            .getJSONObject(0)
            .getJSONObject("parameters")
            .getJSONArray("allowedCardNetworks")

        assertEquals(2, allowedCardNetworks.length())
        assertEquals("ELO", allowedCardNetworks.getString(0))
        assertEquals("ELO_DEBIT", allowedCardNetworks.getString(1))
    }

    @Test
    fun createPaymentAuthRequest_whenManifestInvalid_forwardsExceptionToListener() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createPaymentAuthRequest(baseRequest, intentDataCallback)

        val captor = slot<GooglePayPaymentAuthRequest>()
        verify { intentDataCallback.onGooglePayPaymentAuthRequest(capture(captor)) }

        val request = captor.captured
        assertTrue(request is GooglePayPaymentAuthRequest.Failure)
        val exception = request.error
        assertTrue(exception is BraintreeException)
        assertEquals(
            "GooglePayActivity was not found in the Android manifest, " +
                    "or did not have a theme of R.style.bt_transparent_activity",
            exception.message
        )
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_FAILED, any()) }
    }

    @Test
    fun tokenize_withCardToken_returnsGooglePayNonce() {
        val paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAY_CARD

        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val pd = PaymentData.fromJson(paymentDataJson)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.tokenize(pd, activityResultCallback)

        val captor = slot<GooglePayResult>()
        verify { activityResultCallback.onGooglePayResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is GooglePayResult.Success)
        assertTrue(result.nonce is GooglePayCardNonce)
    }

    @Test
    fun tokenize_withPayPalToken_returnsPayPalAccountNonce() {
        val paymentDataJson = Fixtures.REPSONSE_GOOGLE_PAY_PAYPAL_ACCOUNT

        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .environment("sandbox")
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .paypalClientId("paypal-client-id-for-google-payment")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
                    .enabled(true)
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val pd = PaymentData.fromJson(paymentDataJson)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.tokenize(pd, activityResultCallback)

        val captor = slot<GooglePayResult>()
        verify { activityResultCallback.onGooglePayResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is GooglePayResult.Success)
        assertTrue(result.nonce is PayPalAccountNonce)
    }

    @Test
    fun tokenize_whenPaymentDataExists_returnsResultToListener_andSendsAnalytics() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val paymentDataJson = Fixtures.RESPONSE_GOOGLE_PAY_CARD
        val paymentData = PaymentData.fromJson(paymentDataJson)
        val googlePayPaymentAuthResult = GooglePayPaymentAuthResult(paymentData, null)
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback)

        val captor = slot<GooglePayResult>()
        verify { activityResultCallback.onGooglePayResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is GooglePayResult.Success)
        val nonce = result.nonce
        val expectedNonce = GooglePayCardNonce.fromJSON(JSONObject(paymentDataJson))
        assertEquals(nonce.string, expectedNonce.string)

        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_STARTED, any()) }
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_SUCCEEDED, any()) }
    }

    @Test
    fun tokenize_whenErrorExists_returnsErrorToListener_andSendsAnalytics() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val errorMessage = "Error message"
        val error = Exception(errorMessage)
        val googlePayPaymentAuthResult = GooglePayPaymentAuthResult(null, error)
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback)

        val captor = slot<GooglePayResult>()
        verify { activityResultCallback.onGooglePayResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is GooglePayResult.Failure)
        assertEquals(error, result.error)

        val errorParams = AnalyticsEventParams(
            errorDescription = errorMessage
        )

        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_STARTED, any()) }
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_FAILED, errorParams) }
    }

    @Test
    fun tokenize_whenUserCanceledErrorExists_returnsErrorToListener_andSendsAnalytics() {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val userCanceledError = UserCanceledException("User canceled Google Pay.")
        val googlePayPaymentAuthResult = GooglePayPaymentAuthResult(null, userCanceledError)
        sut.tokenize(googlePayPaymentAuthResult, activityResultCallback)

        val captor = slot<GooglePayResult>()
        verify { activityResultCallback.onGooglePayResult(capture(captor)) }

        val result = captor.captured
        assertTrue(result is GooglePayResult.Cancel)
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_STARTED, any()) }
        verify { braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_SHEET_CANCELED, any()) }
    }

    @Test
    fun getAllowedCardNetworks_returnsSupportedNetworks() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
            )
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString("sandbox_tokenization_string")

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()

        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val allowedCardNetworks = sut.getAllowedCardNetworks(configuration)

        assertEquals(4, allowedCardNetworks.size)
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_VISA))
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_MASTERCARD))
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_AMEX))
        assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_DISCOVER))
    }

    @Test
    fun getTokenizationParameters_returnsCorrectParameters() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        every { merchantRepository.authorization } returns authorization

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()
        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).parameters

        assertEquals("braintree", tokenizationParameters.getString("gateway"))
        assertEquals(configuration.merchantId, tokenizationParameters.getString("braintree:merchantId"))
        assertEquals(
            configuration.googlePayAuthorizationFingerprint,
            tokenizationParameters.getString("braintree:authorizationFingerprint")
        )
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"))
        assertEquals(
            com.braintreepayments.api.core.BuildConfig.VERSION_NAME,
            tokenizationParameters.getString("braintree:sdkVersion")
        )
    }

    @Test
    fun getTokenizationParameters_doesNotIncludeATokenizationKeyWhenNotPresent() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        val authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
        every { merchantRepository.authorization } returns authorization

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()
        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).parameters
        assertNull(tokenizationParameters.getString("braintree:clientKey"))
    }

    @Test
    fun getTokenizationParameters_includesATokenizationKeyWhenPresent() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        every { merchantRepository.authorization } returns authorization

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()
        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        val tokenizationParameters = sut.getTokenizationParameters(configuration, authorization).parameters
        assertEquals(Fixtures.TOKENIZATION_KEY, tokenizationParameters.getString("braintree:clientKey"))
    }

    @Test
    fun getTokenizationParameters_forwardsParametersAndAllowedCardsToCallback() {
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .googlePay(
                TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                    .googleAuthorizationFingerprint("google-auth-fingerprint")
                    .supportedNetworks(arrayOf("visa", "mastercard", "amex", "discover"))
            )
            .withAnalytics()
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .activityInfo(activityInfo)
            .build()

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.TOKENIZATION_KEY)

        val internalGooglePayClient = MockkGooglePayInternalClientBuilder().build()
        val sut = GooglePayClient(
            braintreeClient,
            internalGooglePayClient,
            analyticsParamRepository,
            merchantRepository
        )

        sut.getTokenizationParameters { tokenizationParameters ->
            assertTrue(tokenizationParameters is GooglePayTokenizationParameters.Success)
            assertNotNull(tokenizationParameters.parameters)
            assertEquals(
                sut.getAllowedCardNetworks(configuration),
                tokenizationParameters.allowedCardNetworks
            )
        }
    }
}
