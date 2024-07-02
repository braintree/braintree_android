package com.braintreepayments.api.paypalmessaging

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.ExperimentalBetaApi
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.paypal.messages.config.message.PayPalMessageConfig
import io.mockk.verify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingViewUnitTest {

    private lateinit var context: Context
    private lateinit var listener: PayPalMessagingListener

    @Before
    fun beforeEach() {
        context = ApplicationProvider.getApplicationContext()
        listener = mockk(relaxed = true)
    }

    @Test
    fun `test start with configuration error calls onPayPalMessagingFailure delegate with error`() {
        val configError = Exception("Configuration error.")
        val braintreeClient = MockBraintreeClientBuilder()
            .configurationError(configError)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.setListener(listener)
        payPalMessageView.start()

        val exceptionSlot = slot<Exception>()
        verify { listener.onPayPalMessagingFailure(capture(exceptionSlot)) }
        val capturedException = exceptionSlot.captured
        assertEquals("Configuration error.", capturedException.message)
    }

    @Test
    fun `test start with no client ID calls onPayPalMessagingFailure delegate with error and sends analytics`() {
        val payPalMissingClientIdConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalMissingClientIdConfig)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.setListener(listener)
        payPalMessageView.start()

        val exceptionSlot = slot<Exception>()
        verify { listener.onPayPalMessagingFailure(capture(exceptionSlot)) }
        val capturedException = exceptionSlot.captured
        assertEquals("Could not find PayPal client ID in Braintree configuration.", capturedException.message)
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:failed") }
    }

    @Test
    fun `test start with valid configuration calls onPayPalMessagingLoading delegate and sends analytics`() {
        val payPalConfiguration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        mockkObject(PayPalMessageConfig)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(payPalConfiguration)
            .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.setListener(listener)

        assertEquals(payPalMessageView.childCount, 0)

        payPalMessageView.start()

        verify { listener.onPayPalMessagingLoading() }
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
        verify { PayPalMessageConfig.setGlobalAnalytics(
                integrationName = "BT_SDK",
                integrationVersion = BuildConfig.VERSION_NAME
        ) }

        assertEquals(payPalMessageView.childCount, 1)
    }

    @Test
    fun `test start with valid configuration multiple times does not increase number of subviews`() {
        val payPalConfiguration: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        mockkObject(PayPalMessageConfig)
        val braintreeClient = MockkBraintreeClientBuilder()
                .configurationSuccess(payPalConfiguration)
                .build()

        val payPalMessageView = PayPalMessagingView(braintreeClient, context)
        payPalMessageView.setListener(listener)

        assertEquals(payPalMessageView.childCount, 0)

        payPalMessageView.start()

        assertEquals(payPalMessageView.childCount, 1)
        verify { listener.onPayPalMessagingLoading() }
        verify { braintreeClient.sendAnalyticsEvent("paypal-messaging:create-view:started") }
        verify { PayPalMessageConfig.setGlobalAnalytics(
                integrationName = "BT_SDK",
                integrationVersion = BuildConfig.VERSION_NAME
        ) }

        payPalMessageView.start()

        assertEquals(payPalMessageView.childCount, 1)
    }
}
