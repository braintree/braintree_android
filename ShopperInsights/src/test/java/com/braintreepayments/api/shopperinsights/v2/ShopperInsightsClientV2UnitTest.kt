package com.braintreepayments.api.shopperinsights.v2

import android.content.Context
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.ButtonOrder
import com.braintreepayments.api.shopperinsights.ButtonType
import com.braintreepayments.api.shopperinsights.ExperimentType
import com.braintreepayments.api.shopperinsights.PageType
import com.braintreepayments.api.shopperinsights.PresentmentDetails
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.BUTTON_PRESENTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.BUTTON_SELECTED
import com.braintreepayments.api.shopperinsights.v2.internal.CreateCustomerSessionApi
import com.braintreepayments.api.shopperinsights.v2.internal.CreateCustomerSessionApi.CreateCustomerSessionResult
import com.braintreepayments.api.shopperinsights.v2.internal.UpdateCustomerSessionApi
import com.braintreepayments.api.shopperinsights.v2.internal.UpdateCustomerSessionApi.UpdateCustomerSessionResult as ApiUpdateResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsClientV2UnitTest {

    private val braintreeClient = mockk<BraintreeClient>(relaxed = true)
    private val deviceInspector = mockk<DeviceInspector>(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val createCustomerSessionApi = mockk<CreateCustomerSessionApi>(relaxed = true)
    private val updateCustomerSessionApi = mockk<UpdateCustomerSessionApi>(relaxed = true)

    private val context = mockk<Context>(relaxed = true)

    private lateinit var subject: ShopperInsightsClientV2

    private val sessionId = "session_id"

    @Before
    fun setUp() {
        subject = ShopperInsightsClientV2(
            braintreeClient = braintreeClient,
            createCustomerSessionApi = createCustomerSessionApi,
            updateCustomerSessionApi = updateCustomerSessionApi,
            deviceInspector = deviceInspector,
            lazy { analyticsClient }
        )
    }

    @Test
    fun `when sendPresentedEvent is called, BUTTON_PRESENTED event is sent`() {
        subject.sendPresentedEvent(
            buttonType = ButtonType.PAYPAL,
            presentmentDetails = PresentmentDetails(
                type = ExperimentType.CONTROL,
                buttonOrder = ButtonOrder.FIRST,
                pageType = PageType.CHECKOUT
            ),
            sessionId = sessionId
        )

        verify {
            analyticsClient.sendEvent(
                BUTTON_PRESENTED,
                AnalyticsEventParams(
                    experiment = ExperimentType.CONTROL.formattedExperiment(),
                    shopperSessionId = sessionId,
                    buttonType = ButtonType.PAYPAL.stringValue,
                    buttonOrder = ButtonOrder.FIRST.stringValue,
                    pageType = PageType.CHECKOUT.stringValue
                )
            )
        }
    }

    @Test
    fun `when sendSelectedEvent is called, BUTTON_SELECTED event is sent`() {
        subject.sendSelectedEvent(ButtonType.VENMO, sessionId)

        verify {
            analyticsClient.sendEvent(
                BUTTON_SELECTED,
                AnalyticsEventParams(
                    shopperSessionId = sessionId,
                    buttonType = ButtonType.VENMO.stringValue,
                )
            )
        }
    }

    @Test
    fun `when isPayPalAppInstalled is called, deviceInspector isPayPalInstalled is invoked`() {
        val payPalInstalled = true
        every { deviceInspector.isPayPalInstalled(context) } returns payPalInstalled

        val result = subject.isPayPalAppInstalled(context)

        assertEquals(payPalInstalled, result)
    }

    @Test
    fun `when isVenmoAppInstalled is called, deviceInspector isVenmoAppInstalled is invoked`() {
        val venmoInstalled = true
        every { deviceInspector.isVenmoInstalled(context) } returns venmoInstalled

        val result = subject.isVenmoAppInstalled(context)

        assertEquals(venmoInstalled, result)
    }

    @Test
    fun `when createCustomerSession is called and succeeds, callback is invoked with Success`() {
        val customerSessionRequest = mockk<CustomerSessionRequest>()
        val callbackSlot = slot<(CreateCustomerSessionResult) -> Unit>()
        val sessionId = "test-session-id"

        every {
            createCustomerSessionApi.execute(customerSessionRequest, capture(callbackSlot))
        } answers {
            callbackSlot.captured(CreateCustomerSessionResult.Success(sessionId))
        }

        var result: ShopperInsightsClientV2.CustomerSessionResult? = null
        subject.createCustomerSession(customerSessionRequest) { result = it }

        assert(result is ShopperInsightsClientV2.CustomerSessionResult.Success)
        assertEquals(sessionId, (result as ShopperInsightsClientV2.CustomerSessionResult.Success).sessionId)
    }

    @Test
    fun `when createCustomerSession is called and fails, callback is invoked with Failure`() {
        val customerSessionRequest = mockk<CustomerSessionRequest>()
        val callbackSlot = slot<(CreateCustomerSessionResult) -> Unit>()
        val error = Exception("Test error")

        every {
            createCustomerSessionApi.execute(customerSessionRequest, capture(callbackSlot))
        } answers {
            callbackSlot.captured(CreateCustomerSessionResult.Error(error))
        }

        var result: ShopperInsightsClientV2.CustomerSessionResult? = null
        subject.createCustomerSession(customerSessionRequest) { result = it }

        assert(result is ShopperInsightsClientV2.CustomerSessionResult.Failure)
        assertEquals(error, (result as ShopperInsightsClientV2.CustomerSessionResult.Failure).error)
    }
    @Test
    fun `when updateCustomerSession is called and succeeds, callback is invoked with Success`() {
        val customerSessionRequest = mockk<CustomerSessionRequest>()
        val callbackSlot = slot<(ApiUpdateResult) -> Unit>()
        val sessionId = "test-session-id"

        every {
            updateCustomerSessionApi.execute(customerSessionRequest, sessionId, capture(callbackSlot))
        } answers {
            callbackSlot.captured(ApiUpdateResult.Success(sessionId))
        }

        var result: ShopperInsightsClientV2.CustomerSessionResult? = null
        subject.updateCustomerSession(customerSessionRequest, sessionId) { result = it }

        assert(result is ShopperInsightsClientV2.CustomerSessionResult.Success)
        assertEquals(sessionId, (result as ShopperInsightsClientV2.CustomerSessionResult.Success).sessionId)
    }

    @Test
    fun `when updateCustomerSession is called and fails, callback is invoked with Failure`() {
        val customerSessionRequest = mockk<CustomerSessionRequest>()
        val callbackSlot = slot<(ApiUpdateResult) -> Unit>()
        val sessionId = "test-session-id"
        val error = Exception("Test error")

        every {
            updateCustomerSessionApi.execute(customerSessionRequest, sessionId, capture(callbackSlot))
        } answers {
            callbackSlot.captured(ApiUpdateResult.Error(error))
        }

        var result: ShopperInsightsClientV2.CustomerSessionResult? = null
        subject.updateCustomerSession(customerSessionRequest, sessionId) { result = it }

        assert(result is ShopperInsightsClientV2.CustomerSessionResult.Failure)
        assertEquals(error, (result as ShopperInsightsClientV2.CustomerSessionResult.Failure).error)
    }
}
