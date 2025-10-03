package com.braintreepayments.api.shopperinsights

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.TokenizationKey
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for BraintreeShopperInsightsClient.
 *
 * This class contains tests for the shopper insights functionality within the Braintree SDK.
 * It focuses on testing how the client handles different scenarios when fetching recommended
 * payment methods.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsClientUnitTest {

    private lateinit var sut: ShopperInsightsClient
    private lateinit var api: ShopperInsightsApi
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var merchantRepository: MerchantRepository
    private lateinit var context: Context
    private lateinit var deviceInspector: DeviceInspector
    private var shopperSessionId = "test-shopper-session-id"

    private val clientToken = mockk<ClientToken>()

    @Before
    fun beforeEach() {
        api = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        merchantRepository = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)

        every { merchantRepository.authorization } returns clientToken

        sut = ShopperInsightsClient(
            braintreeClient,
            analyticsParamRepository,
            api,
            merchantRepository,
            deviceInspector,
            shopperSessionId = shopperSessionId
        )
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when getRecommendedPaymentMethods is called without shopper session id, session id is reset`() {
        sut.getRecommendedPaymentMethods(mockk(relaxed = true), "some_experiment", mockk(relaxed = true))

        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun `when getRecommendedPaymentMethods is called, started event is sent`() {
        val experiment = "some_experiment"
        sut.getRecommendedPaymentMethods(mockk(relaxed = true), experiment, mockk(relaxed = true))

        verifyStartedAnalyticsEvent(AnalyticsEventParams(
            experiment = experiment,
            shopperSessionId = shopperSessionId))
    }

    @Test
    fun `when getRecommendedPaymentMethods is called, failed event is sent`() {
        val request = ShopperInsightsRequest(null, null)

        sut.getRecommendedPaymentMethods(request, "some_experiment", mockk(relaxed = true))

        verifyFailedAnalyticsEvent()
    }

    @Test
    fun `testGetRecommendedPaymentMethods - findEligiblePayments is called with request`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)
        val request = ShopperInsightsRequest("some-email", null)

        executeTestForFindEligiblePaymentsApi(
            request = request,
            result = null,
            error = null,
            callback = callback
        )

        verify {
            api.findEligiblePayments(
                request = EligiblePaymentsApiRequest(
                    request,
                    currencyCode = "USD",
                    countryCode = "US",
                    accountDetails = true,
                    constraintType = "INCLUDE",
                    paymentSources = listOf("PAYPAL", "VENMO")
                ),
                callback = any()
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - findEligiblePayments returns an error`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)
        val expectedError = Exception("Expected Exception")

        executeTestForFindEligiblePaymentsApi(
            result = null,
            error = expectedError,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Failure)
                    assertEquals((result as ShopperInsightsResult.Failure).error, expectedError)
                }
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - result is null`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = null,
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Failure)
                    assertTrue {
                        (result as ShopperInsightsResult.Failure).error is BraintreeException
                    }
                    assertEquals(
                        "Required fields missing from API response body",
                        (result as ShopperInsightsResult.Failure).error.message
                    )
                }
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - all methods are null`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(EligiblePaymentMethods(paypal = null, venmo = null)),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Failure)
                    assertTrue {
                        (result as ShopperInsightsResult.Failure).error is BraintreeException
                    }
                    assertEquals(
                        "Required fields missing from API response body",
                        (result as ShopperInsightsResult.Failure).error.message
                    )
                }
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - both paypal and venmo recommended`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)
        val eligiblePaymentMethodDetails = EligiblePaymentMethodDetails(
            canBeVaulted = true,
            eligibleInPayPalNetwork = true,
            recommended = true,
            recommendedPriority = 1
        )

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = eligiblePaymentMethodDetails,
                    venmo = eligiblePaymentMethodDetails
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertEquals(true, success.response.isPayPalRecommended)
                    assertEquals(true, success.response.isVenmoRecommended)
                }
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - paymentDetail null`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = null,
                    venmo = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = true,
                        recommended = true,
                        recommendedPriority = 1
                    )
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertEquals(false, success.response.isPayPalRecommended)
                }
            )
        }
    }

    @Test
    fun `testGetRecommendedPaymentMethods - recommended is false`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = true,
                        recommended = false,
                        recommendedPriority = 1
                    ),
                    venmo = null
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertEquals(false, success.response.isPayPalRecommended)
                }
            )
        }
    }

    @Test
    fun `getRecommendedPaymentMethods paypal's eligibleInPayPalNetwork true, isEligibleInPayPalNetwork is true`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = true,
                        recommended = false,
                        recommendedPriority = 1
                    ),
                    venmo = null
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertTrue(success.response.isEligibleInPayPalNetwork)
                }
            )
        }
    }

    @Test
    fun `getRecommendedPaymentMethods venmo's eligibleInPayPalNetwork true, isEligibleInPayPalNetwork is true`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = null,
                    venmo = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = true,
                        recommended = false,
                        recommendedPriority = 1
                    )
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertTrue(success.response.isEligibleInPayPalNetwork)
                }
            )
        }
    }

    @Test
    fun `getRecommendedPaymentMethods both eligibleInPayPalNetwork false, isEligibleInPayPalNetwork is false`() {
        val callback = mockk<ShopperInsightsCallback>(relaxed = true)

        executeTestForFindEligiblePaymentsApi(
            result = EligiblePaymentsApiResult(
                EligiblePaymentMethods(
                    paypal = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = false,
                        recommended = false,
                        recommendedPriority = 1
                    ),
                    venmo = EligiblePaymentMethodDetails(
                        canBeVaulted = true,
                        eligibleInPayPalNetwork = false,
                        recommended = false,
                        recommendedPriority = 1
                    )
                )
            ),
            error = null,
            callback = callback
        )

        verify {
            callback.onResult(
                withArg { result ->
                    assertTrue(result is ShopperInsightsResult.Success)
                    val success = result as ShopperInsightsResult.Success
                    assertFalse(success.response.isEligibleInPayPalNetwork)
                }
            )
        }
    }

    @Test
    fun `when getRecommendedPaymentMethods is called with null request, succeeded event is sent`() {
        val result = EligiblePaymentsApiResult(
            EligiblePaymentMethods(
                paypal = EligiblePaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = false,
                    recommendedPriority = 1
                ),
                venmo = null
            )
        )

        executeTestForFindEligiblePaymentsApi(
            callback = mockk<ShopperInsightsCallback>(relaxed = true),
            result = result,
            error = null
        )

        verifySuccessAnalyticsEvent()
    }

    @Test
    fun `test getRecommendPaymentMethods is called with a tokenization key, error is sent`() {
        every { merchantRepository.authorization } returns mockk<TokenizationKey>()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        sut = ShopperInsightsClient(
            braintreeClient,
            analyticsParamRepository,
            api,
            merchantRepository,
            deviceInspector
        )

        val request = ShopperInsightsRequest("some-email", null)
        sut.getRecommendedPaymentMethods(request) { result ->
            assertTrue(result is ShopperInsightsResult.Failure)
            assertTrue {
                (result as ShopperInsightsResult.Failure).error is BraintreeException
            }
            assertEquals(
                "Invalid authorization. This feature can only be used with a client token.",
                (result as ShopperInsightsResult.Failure).error.message
            )
        }
    }

    @Test
    fun `test paypal button presented analytics event`() {
        // A Test type, with a button in the first position displayed in the mini cart.
        val presentmentDetails = PresentmentDetails(
            ExperimentType.TEST,
            ButtonOrder.FIRST,
            PageType.MINI_CART
        )

        val params = AnalyticsEventParams(
            experiment = presentmentDetails.type.formattedExperiment(),
            shopperSessionId = shopperSessionId,
            buttonType = ButtonType.PAYPAL.stringValue,
            buttonOrder = presentmentDetails.buttonOrder.stringValue,
            pageType = presentmentDetails.pageType.stringValue
        )
        sut.sendPresentedEvent(
            ButtonType.PAYPAL,
            PresentmentDetails(
                ExperimentType.TEST,
                ButtonOrder.FIRST,
                PageType.MINI_CART
            )
        )
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:button-presented",
            params) }
    }

    @Test
    fun `test venmo button presented analytics event`() {
        // A Control type, with a button in the second position displayed on the homepage.
        val presentmentDetails = PresentmentDetails(
            ExperimentType.CONTROL,
            ButtonOrder.SECOND,
            PageType.HOMEPAGE
        )

        val params = AnalyticsEventParams(
            experiment = presentmentDetails.type.formattedExperiment(),
            shopperSessionId = shopperSessionId,
            buttonType = ButtonType.VENMO.stringValue,
            buttonOrder = presentmentDetails.buttonOrder.stringValue,
            pageType = presentmentDetails.pageType.stringValue
        )
        sut.sendPresentedEvent(
            ButtonType.VENMO,
            PresentmentDetails(
                ExperimentType.CONTROL,
                ButtonOrder.SECOND,
                PageType.HOMEPAGE
            )
        )

        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:button-presented",
            params) }
    }

    @Test
    fun `test paypal selected analytics event`() {
        val params = AnalyticsEventParams(
            shopperSessionId = shopperSessionId,
            buttonType = ButtonType.PAYPAL.stringValue
        )
        sut.sendSelectedEvent(
            ButtonType.PAYPAL
        )
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:button-selected",
            params) }
    }

    @Test
    fun `test venmo selected analytics event`() {
        val params = AnalyticsEventParams(
            shopperSessionId = shopperSessionId,
            buttonType = ButtonType.VENMO.stringValue,
        )
        sut.sendSelectedEvent(
            ButtonType.VENMO,
        )
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:button-selected",
            params) }
    }

    @Test
    fun `test isPayPalAppInstalled returns true when deviceInspector returns true`() {
        every { deviceInspector.isPayPalInstalled() } returns true
        assertTrue(sut.isPayPalAppInstalled(context))
    }

    @Test
    fun `test isVenmoAppInstalled returns true when deviceInspector returns true`() {
        every { deviceInspector.isVenmoInstalled(context) } returns true
        assertTrue(sut.isVenmoAppInstalled(context))
    }

    @Test
    fun `test isPayPalAppInstalled returns false when deviceInspector returns false`() {
        every { deviceInspector.isPayPalInstalled() } returns false
        assertFalse(sut.isPayPalAppInstalled(context))
    }

    @Test
    fun `test isVenmoAppInstalled returns false when deviceInspector returns false`() {
        every { deviceInspector.isVenmoInstalled(context) } returns false
        assertFalse(sut.isVenmoAppInstalled(context))
    }

    private fun executeTestForFindEligiblePaymentsApi(
        callback: ShopperInsightsCallback,
        request: ShopperInsightsRequest = ShopperInsightsRequest("some-email", null),
        result: EligiblePaymentsApiResult?,
        error: Exception?
    ) {
        val apiCallbackSlot = slot<EligiblePaymentsCallback>()
        every { api.findEligiblePayments(any(), capture(apiCallbackSlot)) } just runs

        sut.getRecommendedPaymentMethods(request, "some_experiment", callback)

        apiCallbackSlot.captured.onResult(result = result, error = error)
    }

    private fun verifyStartedAnalyticsEvent(params: AnalyticsEventParams = AnalyticsEventParams()) {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:started", params)
        }
    }

    private fun verifySuccessAnalyticsEvent() {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:succeeded",
                    AnalyticsEventParams(shopperSessionId = shopperSessionId))
        }
    }

    private fun verifyFailedAnalyticsEvent() {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:failed",
                    AnalyticsEventParams(
                        shopperSessionId = shopperSessionId,
                        errorDescription = "One of ShopperInsightsRequest.email or " +
                            "ShopperInsightsRequest.phone must be non-null."
                    )
                )
        }
    }
}
