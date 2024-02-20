package com.braintreepayments.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.Configuration.Companion.fromJson
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalMessagingViewUnitTest {

    private lateinit var context: Context

    @Before
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testStart_withConfigurationError_callsDelegateWithError() {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        val listener = MockPayPalMessagingListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        assertEquals(configError.message, listener.error?.message)
    }

    @Test
    fun testStart_withNoClientID_callsDelegateWithErrorAndSendsAnalytics() {
        val payPalMissingClientIdConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalMissingClientIdConfig)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        val listener = MockPayPalMessagingListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        assertEquals("Could not find PayPal client ID in Braintree configuration.", listener.error?.message)
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
        val listener = MockPayPalMessagingListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        assertTrue(listener.loading)
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
    }
}

class MockPayPalMessagingListener: PayPalMessagingListener {

    var error: Exception? = null
    var loading: Boolean = false

    override fun onPayPalMessagingClick() {
        // not unit testable
    }

    override fun onPayPalMessagingApply() {
        // not unit testable
    }

    override fun onPayPalMessagingLoading() {
        loading = true
    }

    override fun onPayPalMessagingSuccess() {
        // not unit testable
    }

    override fun onPayPalMessagingFailure(error: Exception) {
        this.error = error
    }
}
