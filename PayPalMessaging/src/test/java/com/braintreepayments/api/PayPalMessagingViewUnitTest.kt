package com.braintreepayments.api

import android.content.Context
import com.braintreepayments.api.Configuration.Companion.fromJson
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test


class PayPalMessagingViewUnitTest {
    private lateinit var context: Context

    @Test
    fun testStart_withConfigurationError_callsDelegateWithError() {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        context = mockk(relaxed = true)
        val payPalMessageView = PayPalMessagingView(braintreeClient, this.context)
        val listener = PayPalMessagingMockListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        assertEquals(listener.error?.message, configError.message)
    }

    @Test
    fun testStart_withNoClientID_callsDelegateWithError() {
        val payPalMissingClientIdConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID)
        val braintreeClient = MockBraintreeClientBuilder()
            .configuration(payPalMissingClientIdConfig)
            .build()

        context = mockk(relaxed = true)
        val payPalMessageView = PayPalMessagingView(braintreeClient, this.context)
        val listener = PayPalMessagingMockListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        assertEquals("Could not find PayPal client ID in Braintree configuration.", listener.error?.message)
    }

    @Test
    fun testStart_withClientID_firesWillAppearAndSendsAnalytics() {

        val braintreeClient = MockBraintreeClientBuilder()
            .build()

        context = mockk(relaxed = true)
        val payPalMessageView = PayPalMessagingView(braintreeClient, this.context)
        val listener = PayPalMessagingMockListener()

        payPalMessageView.payPalMessagingListener = listener
        payPalMessageView.start()

        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }

//    XCTAssertTrue(mockDelegate.willAppear)
//    XCTAssertTrue(mockAPIClient.postedAnalyticsEvents.contains(BTPayPalMessagingAnalytics.started))
    }
}

class PayPalMessagingMockListener: PayPalMessagingListener {

    var error: Exception? = null

    override fun onPayPalMessagingClick() {
        TODO("Not yet implemented")
    }

    override fun onPayPalMessagingApply() {
        TODO("Not yet implemented")
    }

    override fun onPayPalMessagingLoading() {
        TODO("Not yet implemented")
    }

    override fun onPayPalMessagingSuccess() {
        TODO("Not yet implemented")
    }

    override fun onPayPalMessagingFailure(error: Exception) {
        this.error = error
    }
}