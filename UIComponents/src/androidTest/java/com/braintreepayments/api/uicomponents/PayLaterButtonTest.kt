package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.uicomponents.test.R as TestR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class PayLaterButtonTest {

    @Test
    fun payLaterButton_canBeInstantiated() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        assertNotNull(button)
    }

    @Test
    fun payLaterButton_isEnabledByDefault() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        assertTrue(button.isEnabled)
    }

    @Test
    fun setButtonColor_toBlack_doesNotThrow() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayLaterButtonColor.Black)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toWhite_doesNotThrow() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayLaterButtonColor.White)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toSameColor_doesNotThrow() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayLaterButtonColor.Blue)
        button.setButtonColor(PayLaterButtonColor.Blue)

        assertNotNull(button)
    }

    @Test
    fun setEnabled_false_disablesButton() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        button.isEnabled = false

        assertFalse(button.isEnabled)
    }

    @Test
    fun setEnabled_trueAfterFalse_reenablesButton() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        button.isEnabled = false

        button.isEnabled = true

        assertTrue(button.isEnabled)
    }

    @Test
    fun payLaterButton_hasContentDescription() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)

        assertEquals(
            context.getString(R.string.pay_later_button_content_description),
            button.contentDescription
        )
    }

    @Test
    fun payLaterButton_defaultColor_isBlue() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        assertSame(PayLaterButtonColor.Blue, button.currentStyle)
    }

    @Test
    fun setButtonColor_updatesCurrentStyle() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())

        listOf(PayLaterButtonColor.Black, PayLaterButtonColor.White, PayLaterButtonColor.Blue).forEach {
            button.setButtonColor(it)

            assertSame(it, button.currentStyle)
        }
    }

    @Test
    fun setPayPalRequest_setsShouldOfferPayLater() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        val request = PayPalCheckoutRequest("10.00", true)

        button.setPayPalRequest(request)

        assertTrue(request.shouldOfferPayLater)
    }

    @Test
    fun click_withoutLaunchCallback_throwsNullPointerException() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        button.setPayPalRequest(PayPalCheckoutRequest("10.00", true))

        assertThrows(NullPointerException::class.java) { button.performClick() }
    }

    @Test
    fun onMeasure_withUnspecifiedSpec_usesDesiredSizeClampedToMinimum() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(maxOf(desiredWidth(context), minWidth(context)), button.measuredWidth)
        assertEquals(maxOf(desiredHeight(context), minHeight(context)), button.measuredHeight)
    }

    @Test
    fun onMeasure_withWrapContentSpec_usesDesiredSizeClampedToMinimum() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(LARGE_BUTTON_SIZE, MeasureSpec.AT_MOST)

        button.measure(spec, spec)

        assertEquals(maxOf(desiredWidth(context), minWidth(context)), button.measuredWidth)
        assertEquals(maxOf(desiredHeight(context), minHeight(context)), button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactSpec_usesProvidedSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val exactWidth = desiredWidth(context) + LARGE_BUTTON_SIZE
        val exactHeight = desiredHeight(context) + LARGE_BUTTON_SIZE
        val widthSpec = MeasureSpec.makeMeasureSpec(exactWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(exactHeight, MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(exactWidth, button.measuredWidth)
        assertEquals(exactHeight, button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactWidthBelowMinimum_clampsToFitLogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight(context), MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minWidth(context), button.measuredWidth)
        assertTrue(button.measuredWidth >= logo(context).intrinsicWidth)
    }

    @Test
    fun onMeasure_withExactHeightBelowMinimum_clampsToFitLogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(desiredWidth(context), MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minHeight(context), button.measuredHeight)
        assertTrue(button.measuredHeight >= logo(context).intrinsicHeight)
    }

    @Test
    fun inflate_withoutColorAttribute_defaultsToBlue() {
        assertSame(PayLaterButtonColor.Blue, inflatedButton(TestR.id.pay_later_default).currentStyle)
    }

    @Test
    fun inflate_withColorAttribute_appliesEachColor() {
        assertSame(PayLaterButtonColor.Blue, inflatedButton(TestR.id.pay_later_blue).currentStyle)
        assertSame(PayLaterButtonColor.Black, inflatedButton(TestR.id.pay_later_black).currentStyle)
        assertSame(PayLaterButtonColor.White, inflatedButton(TestR.id.pay_later_white).currentStyle)
    }

    @Test
    fun inflate_setsContentDescription() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals(
            context.getString(R.string.pay_later_button_content_description),
            inflatedButton(TestR.id.pay_later_black).contentDescription
        )
    }

    @Test
    fun inflate_buttonMeasuresToDesiredSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = inflatedButton(TestR.id.pay_later_white)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(maxOf(desiredWidth(context), minWidth(context)), button.measuredWidth)
        assertEquals(maxOf(desiredHeight(context), minHeight(context)), button.measuredHeight)
    }

    @Test
    fun setButtonColor_afterInflation_overridesXmlColor() {
        val button = inflatedButton(TestR.id.pay_later_black)

        button.setButtonColor(PayLaterButtonColor.White)

        assertSame(PayLaterButtonColor.White, button.currentStyle)
    }

    @Test
    fun setButtonColor_remeasuresAndDrawsWithoutThrowing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        listOf(PayLaterButtonColor.Black, PayLaterButtonColor.White, PayLaterButtonColor.Blue).forEach {
            button.setButtonColor(it)
            button.measure(spec, spec)
            button.layout(0, 0, button.measuredWidth, button.measuredHeight)
            button.draw(Canvas(
                Bitmap.createBitmap(
                    button.measuredWidth, button.measuredHeight, Bitmap.Config.ARGB_8888
                )
            ))
        }

        assertSame(PayLaterButtonColor.Blue, button.currentStyle)
    }

    @Test
    fun setEnabled_false_thenDraw_doesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        button.isEnabled = false
        button.measure(spec, spec)
        button.layout(0, 0, button.measuredWidth, button.measuredHeight)

        button.draw(Canvas(
            Bitmap.createBitmap(
                button.measuredWidth, button.measuredHeight, Bitmap.Config.ARGB_8888
            )
        ))

        assertFalse(button.isEnabled)
    }

    @Test
    fun setPayPalRequest_wiresClickListener() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        assertFalse(button.hasOnClickListeners())

        button.setPayPalRequest(PayPalCheckoutRequest("10.00", true))

        assertTrue(button.hasOnClickListeners())
    }

    @Test
    fun setPayPalRequest_forcesShouldOfferPayLater_evenWhenRequestOptedOut() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        val request = PayPalCheckoutRequest("10.00", false)

        button.setPayPalRequest(request)

        assertTrue(request.shouldOfferPayLater)
    }

    @Test
    fun payPalLaunchCallback_isNullByDefault_andCanBeSet() {
        val button = PayLaterButton(ApplicationProvider.getApplicationContext())
        assertEquals(null, button.payPalLaunchCallback)

        val callback = PayPalLaunchCallback {}
        button.payPalLaunchCallback = callback

        assertSame(callback, button.payPalLaunchCallback)
    }

    @Test
    fun initialize_withActivity_createsClientAndLauncher() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            // Activity result launchers must be registered before the activity is STARTED.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                val button = PayLaterButton(activity)

                button.initialize(
                    activityResultCaller = activity,
                    authorization = Fixtures.TOKENIZATION_KEY,
                    appLinkReturnUrl = Uri.parse("https://example.com/return"),
                    deepLinkFallbackUrlScheme = "com.braintreepayments.demo"
                )

                assertNotNull(button.payPalClient)
                assertNotNull(button.payPalLauncher)
            }
        }
    }

    @Test
    fun click_withInitializedButton_showsLoadingAndDisablesButton() {
        withInitializedButton(authorization = INVALID_AUTHORIZATION) { button ->
            button.payPalLaunchCallback = PayPalLaunchCallback {}
            button.setPayPalRequest(PayPalCheckoutRequest("10.00", true))

            button.performClick()

            // Loading is shown synchronously; the (async) failure can't be delivered until we return.
            assertFalse(button.isEnabled)
        }
    }

    @Test
    fun click_withInvalidAuthorization_reportsFailureAndReenablesButton() {
        val latch = CountDownLatch(1)
        var pendingRequest: PayPalPendingRequest? = null
        var button: PayLaterButton? = null
        withInitializedButton(
            authorization = INVALID_AUTHORIZATION,
            afterBlock = {
                assertTrue(latch.await(CLICK_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                assertTrue(pendingRequest is PayPalPendingRequest.Failure)
                assertTrue(button!!.isEnabled)
            }
        ) {
            button = it
            it.payPalLaunchCallback = PayPalLaunchCallback { request ->
                pendingRequest = request
                latch.countDown()
            }
            it.setPayPalRequest(PayPalCheckoutRequest("10.00", true))

            it.performClick()
        }
    }

    @Test
    fun click_setsShouldOfferPayLater_onRequestThatOptedOut() {
        val request = PayPalCheckoutRequest("10.00", false)
        withInitializedButton(authorization = INVALID_AUTHORIZATION) { button ->
            button.payPalLaunchCallback = PayPalLaunchCallback {}
            button.setPayPalRequest(request)

            button.performClick()
        }

        assertTrue(request.shouldOfferPayLater)
    }

    private fun withInitializedButton(
        authorization: String,
        afterBlock: () -> Unit = {},
        block: (PayLaterButton) -> Unit
    ) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            // Activity result launchers must be registered before the activity is STARTED.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                val button = PayLaterButton(activity)
                button.initialize(
                    activityResultCaller = activity,
                    authorization = authorization,
                    appLinkReturnUrl = Uri.parse("https://example.com/return"),
                    deepLinkFallbackUrlScheme = "com.braintreepayments.demo"
                )
                block(button)
            }
            // Runs while the activity is still alive, so async callbacks can be awaited without sleeping.
            afterBlock()
        }
    }

    private fun inflatedButton(id: Int): PayLaterButton {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = LayoutInflater.from(context).inflate(
            TestR.layout.pay_later_button_colors_test,
            null
        ) as LinearLayout
        return root.findViewById(id)
    }

    private fun desiredWidth(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_width).toInt()

    private fun desiredHeight(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_height).toInt()

    private fun focusPadding(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_focus_padding).toInt()

    private fun logo(context: Context) =
        ContextCompat.getDrawable(context, R.drawable.pay_later_logo_black)!!

    private fun minWidth(context: Context) =
        logo(context).intrinsicWidth + 4 * focusPadding(context)

    private fun minHeight(context: Context) =
        desiredHeight(context)

    companion object {
        private const val LARGE_BUTTON_SIZE = 2000
        private const val INVALID_AUTHORIZATION = "invalid"
        private const val CLICK_TIMEOUT_SECONDS = 5L
    }
}
