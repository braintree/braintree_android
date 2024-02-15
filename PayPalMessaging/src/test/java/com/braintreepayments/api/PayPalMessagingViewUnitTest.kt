package com.braintreepayments.api

import android.content.Context
import io.mockk.mockk
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