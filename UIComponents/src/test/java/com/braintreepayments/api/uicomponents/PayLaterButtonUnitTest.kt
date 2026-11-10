package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayLaterButtonUnitTest {

    private lateinit var context: Context

    private val mockPayPalClient = mockk<PayPalClient>()
    private val mockPayPalLauncher = mockk<PayPalLauncher>()
    private val analyticsClient = mockk<AnalyticsClient>(relaxed = true)
    private val authCallback = slot<PayPalPaymentAuthCallback>()
    private val launchResults = mutableListOf<PayPalPendingRequest>()
    private val request = PayPalCheckoutRequest("10.00", true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        every { mockPayPalClient.createPaymentAuthRequest(any(), any(), capture(authCallback)) } just runs
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
    }

    @After
    fun tearDown() {
        unmockkObject(AnalyticsClient.Companion)
    }

    // region PayLaterButtonColor
    @Test
    fun `each color maps to PayPal styling with its own wordmark`() {
        listOf(
            Triple(PayLaterButtonColor.Blue, PayPalButtonColor.Blue, R.drawable.pay_later_logo_black),
            Triple(PayLaterButtonColor.Black, PayPalButtonColor.Black, R.drawable.pay_later_logo_white),
            Triple(PayLaterButtonColor.White, PayPalButtonColor.White, R.drawable.pay_later_logo_black),
        ).forEach { (payLater, payPal, logo) ->
            assertEquals(logo, payLater.logoId)
            assertEquals(payPal.toStyle().copy(logoId = logo), payLater.toStyle())
        }
    }

    @Test
    fun `fromId maps keys to colors and falls back to blue`() {
        assertEquals(PayLaterButtonColor.Blue, PayLaterButtonColor.fromId(0))
        assertEquals(PayLaterButtonColor.Black, PayLaterButtonColor.fromId(1))
        assertEquals(PayLaterButtonColor.White, PayLaterButtonColor.fromId(2))
        assertEquals(PayLaterButtonColor.Blue, PayLaterButtonColor.fromId(UNKNOWN_COLOR_KEY))
    }

    @Test
    fun `wordmark drawables load at 155dp by 27dp`() {
        val density = context.resources.displayMetrics.density
        listOf(R.drawable.pay_later_logo_black, R.drawable.pay_later_logo_white).forEach { id ->
            val drawable = ContextCompat.getDrawable(context, id)

            assertNotNull(drawable)
            assertEquals((LOGO_WIDTH_DP * density).toInt(), drawable!!.intrinsicWidth)
            assertEquals((LOGO_HEIGHT_DP * density).toInt(), drawable.intrinsicHeight)
        }
    }

    // endregion

    // region XML attribute

    @Test
    fun `defaults to blue when no color attribute is set`() {
        val button = PayLaterButton(context)

        assertEquals(PayLaterButtonColor.Blue, button.currentStyle)
    }

    @Test
    fun `paymentButtonColor black attribute sets black`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paymentButtonColor, "black")
            .build()

        val button = PayLaterButton(context, attrs)

        assertEquals(PayLaterButtonColor.Black, button.currentStyle)
    }

    @Test
    fun `paymentButtonColor white attribute sets white`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paymentButtonColor, "white")
            .build()

        val button = PayLaterButton(context, attrs)

        assertEquals(PayLaterButtonColor.White, button.currentStyle)
    }

    @Test
    fun `setButtonColor updates the color`() {
        val button = PayLaterButton(context)

        button.setButtonColor(PayLaterButtonColor.Black)
        assertEquals(PayLaterButtonColor.Black, button.currentStyle)

        button.setButtonColor(PayLaterButtonColor.White)
        assertEquals(PayLaterButtonColor.White, button.currentStyle)
    }

    // endregion

    // region Accessibility

    @Test
    fun `contentDescription is PayPal Pay Later`() {
        val button = PayLaterButton(context)

        assertEquals("PayPal Pay Later", button.contentDescription.toString())
    }

    // endregion

    // region Request

    @Test
    fun `setPayPalRequest sets shouldOfferPayLater to true`() {
        val button = PayLaterButton(context)
        val request = PayPalCheckoutRequest("10.00", true)
        assertFalse(request.shouldOfferPayLater)

        button.setPayPalRequest(request)

        assertTrue(request.shouldOfferPayLater)
    }

    // endregion

    // region Size

    @Test
    fun `measures to the shared payment button height and width`() {
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(dimen(R.dimen.pay_button_height), button.measuredHeight)
        assertEquals(maxOf(dimen(R.dimen.pay_button_width), minWidth()), button.measuredWidth)
    }

    @Test
    fun `width below minimum clamps to fit the wordmark`() {
        val button = PayLaterButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(dimen(R.dimen.pay_button_height), MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minWidth(), button.measuredWidth)
        assertTrue(button.measuredWidth > logo().intrinsicWidth)
    }

    // endregion

    // region Re-enable after failure

    @Test
    fun `auth request failure re-enables the button`() {
        val button = buttonWith(activity())
        button.performClick()
        assertFalse(button.isEnabled)

        val error = Exception("auth request failed")
        authCallback.captured.onPayPalPaymentAuthRequest(authRequestFailure(error))

        assertTrue(button.isEnabled)
        assertSame(error, (launchResults.single() as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `launch failure re-enables the button`() {
        val button = buttonWith(activity())
        val error = Exception("launch failed")
        every { mockPayPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Failure(error)
        button.performClick()
        assertFalse(button.isEnabled)

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        assertTrue(button.isEnabled)
        assertSame(error, (launchResults.single() as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `missing activity re-enables the button`() {
        val button = buttonWith(context)
        button.performClick()
        assertFalse(button.isEnabled)

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        assertTrue(button.isEnabled)
        val error = (launchResults.single() as PayPalPendingRequest.Failure).error
        assertTrue(error is NullPointerException)
        assertEquals("Activity is null", error.message)
        verify(exactly = 0) { mockPayPalLauncher.launch(any(), any()) }
    }

    // endregion

    // region Launch and return flow

    @Test
    fun `click without a launch callback throws NullPointerException`() {
        val button = buttonWith(activity(), withCallback = false)

        val exception = assertThrows(NullPointerException::class.java) { button.performClick() }
        assertEquals("PayPalLaunchCallback must be initialized first", exception.message)
    }

    @Test
    fun `click creates the payment auth request with the checkout request`() {
        val button = buttonWith(activity())

        button.performClick()

        verify { mockPayPalClient.createPaymentAuthRequest(any(), request, any()) }
    }

    @Test
    fun `launch started forwards the pending request and keeps the button loading`() {
        every { mockPayPalLauncher.launch(any(), any()) } returns PayPalPendingRequest.Started("pending-request")
        val button = buttonWith(activity())
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        assertEquals("pending-request", (launchResults.single() as PayPalPendingRequest.Started).pendingRequestString)
        assertFalse(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Success tokenizes, forwards the result and re-enables`() {
        val authSuccess = mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns authSuccess
        val tokenized = mockk<PayPalResult.Success>(relaxed = true)
        every { mockPayPalClient.tokenize(authSuccess, any()) } answers {
            secondArg<PayPalTokenizeCallback>().onPayPalResult(tokenized)
        }
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(tokenized, results.single())
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on NoResult returns Cancel and re-enables`() {
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(PayPalResult.Cancel, results.single())
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp on Failure returns the error and re-enables`() {
        val error = Exception("return error")
        val authFailure = mockk<PayPalPaymentAuthResult.Failure>()
        every { authFailure.error } returns error
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns authFailure
        val results = mutableListOf<PayPalResult>()
        val button = buttonWith(context)
        button.isEnabled = false

        button.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent()) { results.add(it) }

        assertSame(error, (results.single() as PayPalResult.Failure).error)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `handleReturnToApp passes the pending request and intent to the launcher`() {
        every { mockPayPalLauncher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        val button = buttonWith(context)
        val pending = PayPalPendingRequest.Started("pending")
        val intent = Intent("test")

        button.handleReturnToApp(pending, intent) {}

        verify { mockPayPalLauncher.handleReturnToApp(pending, intent) }
    }

    @Test
    fun `setButtonColor recolors the background`() {
        val button = PayLaterButton(context)

        button.setButtonColor(PayLaterButtonColor.Black)

        assertEquals(
            ContextCompat.getColor(context, PayLaterButtonColor.Black.toStyle().default.fill),
            fillColor(button)
        )
    }

    // endregion

    // region Analytics

    @Test
    fun `initialize sends the presented analytics event`() {
        PayLaterButton(context).initialize(
            activityResultCaller = Robolectric.buildActivity(ComponentActivity::class.java).create().get(),
            authorization = "sandbox_tokenization_key",
            appLinkReturnUrl = Uri.parse("https://example.com/return"),
            deepLinkFallbackUrlScheme = "com.braintreepayments.demo"
        )

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAY_LATER_BUTTON_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_XML_VIEW)
            )
        }
    }

    @Test
    fun `launching the flow sends the selected analytics event`() {
        val button = buttonWith(activity())
        every {
            mockPayPalLauncher.launch(any(), any())
        } returns PayPalPendingRequest.Started("pending-request")
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAY_LATER_BUTTON_SELECTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_XML_VIEW)
            )
        }
    }

    @Test
    fun `missing activity does not send the selected analytics event`() {
        val button = buttonWith(context)
        button.performClick()

        authCallback.captured.onPayPalPaymentAuthRequest(mockk<PayPalPaymentAuthRequest.ReadyToLaunch>())

        verify(exactly = 0) {
            analyticsClient.sendEvent(UIComponentsAnalytics.PAY_LATER_BUTTON_SELECTED, any(), any())
        }
    }

    // endregion

    private fun activity() = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    private fun buttonWith(buttonContext: Context, withCallback: Boolean = true) =
        PayLaterButton(buttonContext).apply {
            payPalClient = mockPayPalClient
            payPalLauncher = mockPayPalLauncher
            if (withCallback) payPalLaunchCallback = PayPalLaunchCallback { launchResults.add(it) }
            setPayPalRequest(request)
        }

    private fun fillColor(button: PayLaterButton): Int {
        val layers = button.background as LayerDrawable
        return (layers.getDrawable(1) as GradientDrawable).color!!.defaultColor
    }

    private fun authRequestFailure(error: Exception): PayPalPaymentAuthRequest.Failure {
        val failure = mockk<PayPalPaymentAuthRequest.Failure>()
        every { failure.error } returns error
        return failure
    }

    private fun dimen(id: Int) = context.resources.getDimension(id).toInt()

    private fun logo() = ContextCompat.getDrawable(context, R.drawable.pay_later_logo_black)!!

    private fun minWidth() = logo().intrinsicWidth + 4 * dimen(R.dimen.pay_button_focus_padding)

    companion object {
        private const val UNKNOWN_COLOR_KEY = 99
        private const val LOGO_WIDTH_DP = 155
        private const val LOGO_HEIGHT_DP = 27
    }
}
