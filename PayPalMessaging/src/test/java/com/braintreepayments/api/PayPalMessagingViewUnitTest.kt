package com.braintreepayments.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.Configuration.Companion.fromJson
import io.mockk.verify
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalMessagingViewUnitTest {

    private lateinit var context: Context
    private lateinit var listener: PayPalMessagingListener

    @Before
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        listener = mockk(relaxed = true)
    }

    @Test
    fun testStart_withConfigurationError_callsDelegateWithError() {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        val exceptionSlot = slot<Exception>()
        verify { listener.onPayPalMessagingFailure(capture(exceptionSlot)) }
        val capturedException = exceptionSlot.captured
        assertEquals("Configuration error.", capturedException.message)
    }

    @Test
    fun testStart_withNoClientID_callsDelegateWithErrorAndSendsAnalytics() {
        val payPalMissingClientIdConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalMissingClientIdConfig)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        val exceptionSlot = slot<Exception>()
        verify { listener.onPayPalMessagingFailure(capture(exceptionSlot)) }
        val capturedException = exceptionSlot.captured
        assertEquals("Could not find PayPal client ID in Braintree configuration.", capturedException.message)
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:failed") }
    }

    @Test
    fun testStart_withClientID_firesWillAppearAndSendsAnalytics() {
        val payPalConfiguration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalConfiguration)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        verify { listener.onPayPalMessagingLoading() }
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
    }
}
