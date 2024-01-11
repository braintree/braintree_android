package com.braintreepayments.api

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

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
    private lateinit var shopperInsightsApi: ShopperInsightsApi
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var deviceInspector: DeviceInspector

    @Before
    fun beforeEach() {
        shopperInsightsApi = mockk(relaxed = true)
        braintreeClient = mockk(relaxed = true)
        deviceInspector = mockk(relaxed = true)
        every { context.applicationContext } returns applicationContext
        sut = ShopperInsightsClient(shopperInsightsApi, braintreeClient, deviceInspector)
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
}
