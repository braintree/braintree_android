package com.braintreepayments.api.shopperinsights

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.testutils.Fixtures
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
    private lateinit var context: Context

    @Before
    fun beforeEach() {
        api = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        sut = ShopperInsightsClient(braintreeClient, analyticsParamRepository, api)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when getRecommendedPaymentMethods is called, session id is reset`() {
        sut.getRecommendedPaymentMethods(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        verify { analyticsParamRepository.resetSessionId() }
    }

    @Test
    fun `when getRecommendedPaymentMethods is called, started event is sent`() {
        sut.getRecommendedPaymentMethods(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        verifyStartedAnalyticsEvent()
    }

    @Test
    fun `when getRecommendedPaymentMethods is called, failed event is sent`() {
        val request = ShopperInsightsRequest(null, null)

        sut.getRecommendedPaymentMethods(request, mockk(relaxed = true), mockk(relaxed = true))

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
                    assertTrue { result is ShopperInsightsResult.Failure }
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
                    assertTrue { result is ShopperInsightsResult.Failure }
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
                    assertTrue { result is ShopperInsightsResult.Failure }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
                    assertTrue { result is ShopperInsightsResult.Success }
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
        val braintreeClient = MockkBraintreeClientBuilder()
            .authorizationSuccess(Authorization.fromString(Fixtures.TOKENIZATION_KEY))
            .build()

        sut = ShopperInsightsClient(braintreeClient, analyticsParamRepository, api)

        val request = ShopperInsightsRequest("some-email", null)
        sut.getRecommendedPaymentMethods(request) { result ->
            assertTrue { result is ShopperInsightsResult.Failure }
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
    fun `test paypal presented analytics event`() {
        sut.sendPayPalPresentedEvent()
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:paypal-presented") }
    }

    @Test
    fun `test paypal selected analytics event`() {
        sut.sendPayPalSelectedEvent()
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:paypal-selected") }
    }

    @Test
    fun `test venmo presented analytics event`() {
        sut.sendVenmoPresentedEvent()
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:venmo-presented") }
    }

    @Test
    fun `test venmo selected analytics event`() {
        sut.sendVenmoSelectedEvent()
        verify { braintreeClient.sendAnalyticsEvent("shopper-insights:venmo-selected") }
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

    private fun verifyStartedAnalyticsEvent() {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:started")
        }
    }

    private fun verifySuccessAnalyticsEvent() {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:succeeded")
        }
    }

    private fun verifyFailedAnalyticsEvent() {
        verify {
            braintreeClient
                .sendAnalyticsEvent("shopper-insights:get-recommended-payments:failed")
        }
    }
}
