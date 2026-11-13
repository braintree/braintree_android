@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodContentViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val sut = PayPalSavedPaymentMethodContentView(context)
    private val logo: View = sut.findViewById(R.id.paypal_saved_payment_method_logo)
    private val label: TextView = sut.findViewById(R.id.paypal_saved_payment_method_label)
    private val shimmer: View = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_shimmer)
    private val chip: View = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_chip)
    private val creditMessaging: View = sut.findViewById(R.id.paypal_saved_payment_method_credit_messaging)

    @Test
    fun `hidden renders nothing`() {
        render(PayPalSavedPaymentMethodDisplayState.Hidden)

        assertFalse(sut.isVisible)
    }

    @Test
    fun `an error with the logo and label both hidden renders nothing`() {
        val noBrand = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false)

        render(PayPalSavedPaymentMethodDisplayState.Error, style = noBrand)
        assertFalse(sut.isVisible)

        render(CARD_STATE, style = noBrand)
        assertTrue(sut.isVisible)
    }

    @Test
    fun `loading shows the funding instrument shimmer and the Pay Later shimmer`() {
        render(PayPalSavedPaymentMethodDisplayState.Loading, PayPalCreditMessagingDisplayState.Loading)

        assertTrue(shimmer.isVisible)
        assertFalse(chip.isVisible)
        assertTrue(creditMessaging.isVisible)
    }

    @Test
    fun `a funding instrument shows the chip and the Pay Later message`() {
        render(CARD_STATE, MESSAGE_STATE)

        assertFalse(shimmer.isVisible)
        assertTrue(chip.isVisible)
        assertTrue(creditMessaging.isVisible)
    }

    @Test
    fun `an error keeps only the logo and label`() {
        render(PayPalSavedPaymentMethodDisplayState.Error, MESSAGE_STATE)

        assertTrue(sut.isVisible)
        assertTrue(logo.isVisible)
        assertTrue(label.isVisible)
        assertFalse(chip.isVisible)
        assertFalse(creditMessaging.isVisible)
    }

    @Test
    fun `Pay Later is hidden after an edit or when the style turns it off`() {
        render(CARD_STATE, MESSAGE_STATE, isEditCompleted = true)
        assertFalse(creditMessaging.isVisible)

        render(CARD_STATE, MESSAGE_STATE, style = PayPalSavedPaymentMethodViewStyle(showPayPalCreditMessaging = false))
        assertFalse(creditMessaging.isVisible)
    }

    @Test
    fun `Pay Later is laid out under the label when the view was measured before its content arrived`() {
        // Attached and laid out while empty, as in an app: the margins resolve before the content is rendered.
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setContentView(sut)
        layOut()

        render(CARD_STATE, MESSAGE_STATE)
        layOut()

        val labelStart = startInContent(label) + label.paddingStart
        assertEquals(labelStart, startInContent(creditMessaging))
    }

    @Test
    fun `Pay Later lines up with the label past the logo, or at the leading edge without the logo`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = PayPalSavedPaymentMethodContainerStyle(
                logo = PayPalLogoStyle(widthDp = 48f),
                label = PayPalLabelStyle(marginStartDp = 6f)
            )
        )
        render(CARD_STATE, MESSAGE_STATE, style = style)
        layOut()
        assertEquals(48f.dpToPx(context.resources), logo.layoutParams.width)
        assertEquals(48f.dpToPx(context.resources), logo.layoutParams.height)
        assertEquals(6f.dpToPx(context.resources), label.paddingStart)
        assertEquals(54f.dpToPx(context.resources), (creditMessaging.layoutParams as MarginLayoutParams).marginStart)

        render(CARD_STATE, MESSAGE_STATE, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false))
        layOut()
        assertFalse(logo.isVisible)
        assertEquals(0, label.paddingStart)
        assertEquals(0, (creditMessaging.layoutParams as MarginLayoutParams).marginStart)
    }

    @Test
    fun `with the logo shown and the label hidden, Pay Later lines up with the chip`() {
        render(CARD_STATE, MESSAGE_STATE, style = LOGO_ONLY)

        layOut()

        assertEquals(startInContent(chip), startInContent(creditMessaging))
    }

    @Test
    fun `with the label hidden and the chip stacked, Pay Later starts at the leading edge with the chip`() {
        // A logo almost as wide as the view leaves no room beside it, so the chip stacks below.
        val wideLogo = PayPalSavedPaymentMethodViewStyle(
            showPayPalLabel = false,
            container = PayPalSavedPaymentMethodContainerStyle(logo = PayPalLogoStyle(widthDp = 260f))
        )
        render(CARD_STATE, MESSAGE_STATE, style = wideLogo)

        layOut(width = NARROW_WIDTH)

        assertEquals(sut.paddingStart, startInContent(chip))
        assertEquals(startInContent(chip), startInContent(creditMessaging))
    }

    @Test
    fun `the logo is read as PayPal logo and the label as PayPal`() {
        render(CARD_STATE)
        assertEquals("PayPal logo", logo.contentDescription)
        assertNull(label.contentDescription)
        assertEquals("PayPal", label.text.toString())

        render(CARD_STATE, style = PayPalSavedPaymentMethodViewStyle(showPayPalLabel = false))
        assertFalse(label.isVisible)
        assertEquals("PayPal logo", logo.contentDescription)
    }

    @Test
    fun `the merchant's height is a minimum`() {
        val style =
            PayPalSavedPaymentMethodViewStyle(container = PayPalSavedPaymentMethodContainerStyle(heightDp = 80f))
        render(CARD_STATE, style = style)

        assertEquals(80f.dpToPx(context.resources), sut.minimumHeight)
    }

    // A dense screen, where the chip's dp size is well above 48 pixels.
    @Config(qualifiers = "xxhdpi")
    @Test
    fun `tapping just outside an editable chip still edits, up to a 48dp touch target`() {
        var edits = 0
        sut.onEditClick = { edits++ }
        // Attached, since the chip posts its click.
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get().setContentView(sut)
        render(CARD_STATE)
        layOut()
        val chipBounds = Rect(0, 0, chip.width, chip.height).also { sut.offsetDescendantRectToMyCoords(chip, it) }
        val minSize = 48f.dpToPx(context.resources)
        assertTrue("the chip is smaller than the target", chipBounds.height() < minSize)
        val x = chipBounds.centerX().toFloat()

        tap(x, chipBounds.top - 2f)
        assertEquals(1, edits)
        // Beyond the 48dp target, nothing happens.
        tap(x, chipBounds.centerY() - minSize / 2f - 2f)
        assertEquals(1, edits)
    }

    @Test
    fun `there is no touch target when the chip is hidden`() {
        render(CARD_STATE)
        layOut()
        assertTrue(sut.touchDelegate != null)

        render(PayPalSavedPaymentMethodDisplayState.Error)
        layOut()

        assertNull(sut.touchDelegate)
    }

    // Where [view] starts within the content view.
    private fun startInContent(view: View): Int =
        Rect(0, 0, view.width, view.height).also { sut.offsetDescendantRectToMyCoords(view, it) }.left

    private fun layOut(width: Int = 1080) {
        sut.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        sut.layout(0, 0, sut.measuredWidth, sut.measuredHeight)
    }

    private fun tap(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        sut.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0))
        sut.dispatchTouchEvent(MotionEvent.obtain(time, time + 10, MotionEvent.ACTION_UP, x, y, 0))
        idle()
    }

    private fun render(
        fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
        creditMessagingState: PayPalCreditMessagingDisplayState = PayPalCreditMessagingDisplayState.Hidden,
        isEditCompleted: Boolean = false,
        style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle()
    ) = sut.render(
        fundingInstrumentState = fundingInstrumentState,
        creditMessagingState = creditMessagingState,
        resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style),
        editEnabled = true,
        isEditCompleted = isEditCompleted
    )

    private companion object {
        val CARD_STATE = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        )
        const val NARROW_WIDTH = 300
        val LOGO_ONLY = PayPalSavedPaymentMethodViewStyle(showPayPalLabel = false)
        val MESSAGE_STATE = PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4.", null))
    }
}
