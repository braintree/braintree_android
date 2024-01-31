package com.braintreepayments.api

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.NullPointerException

/**
 * Unit tests for BraintreeShopperInsightsClient.
 *
 * This class contains tests for the shopper insights functionality within the Braintree SDK.
 * It focuses on testing how the client handles different scenarios when fetching recommended
 * payment methods.
 */
class ShopperInsightsClientUnitTest {

    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private lateinit var sut: ShopperInsightsClient
    private lateinit var api: ShopperInsightsApi
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var deviceInspector: DeviceInspector

    @Before
    fun beforeEach() {
        api = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        every { context.applicationContext } returns applicationContext
        sut = ShopperInsightsClient(api, braintreeClient, deviceInspector)
    }

    /**
     * Tests if the getRecommendedPaymentMethods method returns paypal and venmo recommendations
     * when providing a shopping insight request.
     */
    @Test
    fun testGetRecommendedPaymentMethods_noInstalledApps_returnsDefaultRecommendations() {
        every { deviceInspector.isVenmoInstalled(applicationContext) } returns false
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns false

        val request = ShopperInsightsRequest("fake-email", null)
        sut.getRecommendedPaymentMethods(context, request) { result ->
            assertNotNull(result)
            val successResult = assertIs<ShopperInsightsResult.Success>(result)
            assertFalse(successResult.response.isPayPalRecommended)
            assertFalse(successResult.response.isVenmoRecommended)
        }
        verifyStartedAnalyticsEvent()
        verifySuccessAnalyticsEvent()
    }

    @Test
    fun testGetRecommendedPaymentMethods_oneInstalledApp_returnsDefaultRecommendations() {
        every { deviceInspector.isVenmoInstalled(applicationContext) } returns true
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns false

        val request = ShopperInsightsRequest("fake-email", null)
        sut.getRecommendedPaymentMethods(context, request) { result ->
            assertNotNull(result)
            val successResult = assertIs<ShopperInsightsResult.Success>(result)
            assertFalse(successResult.response.isPayPalRecommended)
            assertFalse(successResult.response.isVenmoRecommended)
        }
        verifyStartedAnalyticsEvent()
        verifySuccessAnalyticsEvent()
    }

    @Test
    fun testGetRecommendedPaymentMethods_hasBothAppsInstalled_returnsSuccessResult() {
        every { deviceInspector.isVenmoInstalled(applicationContext) } returns true
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns true

        val request = ShopperInsightsRequest("some-email", null)
        sut.getRecommendedPaymentMethods(context, request) { result ->
            assertNotNull(result)
            val successResult = assertIs<ShopperInsightsResult.Success>(result)
            assertTrue(successResult.response.isPayPalRecommended)
            assertTrue(successResult.response.isVenmoRecommended)
        }
        verifyStartedAnalyticsEvent()
        verifySuccessAnalyticsEvent()
    }

    @Test
    fun `testGetRecommendedPaymentMethods - request object has null properties`() {
        every { deviceInspector.isVenmoInstalled(applicationContext) } returns false
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns true

        val request = ShopperInsightsRequest(null, null)
        sut.getRecommendedPaymentMethods(context, request) { result ->
            assertNotNull(result)
            val error = assertIs<ShopperInsightsResult.Failure>(result)
            val iae = assertIs<IllegalArgumentException>(error.error)
            assertEquals(
                "One of ShopperInsightsRequest.email or " +
                        "ShopperInsightsRequest.phone must be non-null.",
                iae.message
            )
        }
        verifyStartedAnalyticsEvent()
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
                    merchantId = "MXSJ4F5BADVNS",
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
                        (result as ShopperInsightsResult.Failure).error is NullPointerException
                    }
                    assertEquals(
                        "Missing data in API response",
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
                        (result as ShopperInsightsResult.Failure).error is NullPointerException
                    }
                    assertEquals(
                        "Missing data in API response",
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
        every { deviceInspector.isVenmoInstalled(applicationContext) } returns false
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns false

        val apiCallbackSlot = slot<EligiblePaymentsCallback>()
        every { api.findEligiblePayments(any(), capture(apiCallbackSlot)) } just runs

        sut.getRecommendedPaymentMethods(context, request, callback)

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
