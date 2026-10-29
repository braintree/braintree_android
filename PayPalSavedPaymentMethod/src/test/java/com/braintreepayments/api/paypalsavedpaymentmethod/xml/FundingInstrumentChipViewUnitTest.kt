@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodFundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FundingInstrumentChipViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val sut = FundingInstrumentChipView(context)
    private val text: TextView = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_text)
    private val icon: ImageView = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_icon)
    private val editIcon: ImageView = sut.findViewById(R.id.paypal_saved_payment_method_edit_icon)
    private var editClicks = 0

    @Test
    fun `a card shows its masked digits, icon and edit icon, and reads its digits one by one`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)))

        assertEquals("••1234", text.text.toString())
        assertEquals("Visa, ending in 1 2 3 4", sut.contentDescription)
        assertTrue(icon.isVisible)
        assertTrue(editIcon.isVisible)
    }

    @Test
    fun `screen readers hear PayPal named first when the logo and label are both hidden`() {
        val style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false)

        bind(
            PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)),
            style = style
        )

        assertEquals("PayPal, Visa, ending in 1 2 3 4", sut.contentDescription)
    }

    @Test
    fun `screen readers don't hear PayPal again when the label still names it`() {
        val style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = true)

        bind(
            PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)),
            style = style
        )

        assertEquals("Visa, ending in 1 2 3 4", sut.contentDescription)
    }

    @Test
    fun `PayPal Credit shows its label without digits or an icon`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT)))

        assertEquals("Visa", text.text.toString())
        assertEquals("Visa", sut.contentDescription)
        assertFalse(icon.isVisible)
    }

    @Test
    fun `an editable chip shows touch feedback and calls back when tapped`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)))

        assertNotNull(sut.foreground)
        assertTrue(sut.performClick())
        assertEquals(1, editClicks)
    }

    @Test
    fun `binding the same icon again keeps the current image`() {
        val card = PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD))
        bind(card)
        val shown = icon.drawable

        bind(card)

        assertNotNull(shown)
        assertSame(shown, icon.drawable)
    }

    @Test
    fun `a long label shrinks so the edit icon keeps its space`() {
        val longLabel = paymentMethod(PayPalSavedPaymentMethodType.PAYPAL_CREDIT).copy(label = "A".repeat(200))
        bind(PayPalSavedPaymentMethodDisplayState.Content(longLabel))

        sut.measure(
            View.MeasureSpec.makeMeasureSpec(CHIP_WIDTH, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        sut.layout(0, 0, sut.measuredWidth, sut.measuredHeight)

        assertTrue(editIcon.width > 0)
        assertTrue(editIcon.right <= sut.width - sut.paddingRight)
    }

    @Test
    fun `an email the buyer can't edit has no edit icon and isn't tappable`() {
        bind(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false))

        assertEquals("buyer@example.com", text.text.toString())
        assertEquals("buyer@example.com", sut.contentDescription)
        assertFalse(icon.isVisible)
        assertFalse(editIcon.isVisible)
        assertNull(sut.foreground)
        assertFalse(sut.isClickable)
        sut.performClick()
        assertEquals(0, editClicks)
    }

    @Test
    fun `an email the buyer can edit has the edit icon and is tappable`() {
        bind(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true))

        assertTrue(editIcon.isVisible)
        assertTrue(sut.isClickable)
    }

    @Test
    fun `the chip is disabled while an edit is in progress`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)), false)

        assertFalse(sut.isEnabled)
    }

    @Test
    fun `a disabled chip ignores programmatic and accessibility clicks`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)), false)

        sut.performClick()
        ViewCompat.performAccessibilityAction(sut, AccessibilityNodeInfoCompat.ACTION_CLICK, null)

        assertEquals(0, editClicks)
    }

    @Test
    fun `a state without a funding instrument clears the previous one`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)))

        bind(PayPalSavedPaymentMethodDisplayState.Loading)

        assertEquals("", text.text.toString())
        assertFalse(icon.isVisible)
        assertNull(icon.drawable)
    }

    @Test
    fun `the text keeps its fixed color whatever the style's text color`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = PayPalSavedPaymentMethodAppearance(textColor = Color.WHITE)
        )

        val card = PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD))
        bind(card, style = style)

        assertEquals(
            ContextCompat.getColor(context, R.color.paypal_saved_payment_method_funding_instrument_chip_text),
            text.currentTextColor
        )
    }

    @Test
    fun `screen readers get a button with the change payment method action only when editable`() {
        bind(PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD)))
        val editable = nodeInfo()
        assertEquals(Button::class.java.name, editable.className)
        assertEquals("Change payment method", clickActionLabel(editable))

        bind(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false))
        assertNull(clickActionLabel(nodeInfo()))
    }

    @Test
    fun `the edit icon uses the style's size`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = PayPalSavedPaymentMethodContainerStyle(
                fundingInstrument = PayPalSavedPaymentMethodFundingInstrumentStyle(editIconSizeDp = 20f)
            )
        )

        val card = PayPalSavedPaymentMethodDisplayState.Content(paymentMethod(PayPalSavedPaymentMethodType.CARD))
        bind(card, editEnabled = true, style = style)

        assertEquals(20f.dpToPx(context.resources), editIcon.layoutParams.width)
        assertEquals(20f.dpToPx(context.resources), editIcon.layoutParams.height)
    }

    private fun bind(
        state: PayPalSavedPaymentMethodDisplayState,
        editEnabled: Boolean = true,
        style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle()
    ) = sut.bind(state, PayPalSavedPaymentMethodStyleResolver(context, style), editEnabled) { editClicks++ }

    private fun nodeInfo() = AccessibilityNodeInfoCompat.obtain().also {
        ViewCompat.onInitializeAccessibilityNodeInfo(sut, it)
    }

    private fun clickActionLabel(info: AccessibilityNodeInfoCompat): CharSequence? =
        info.actionList.firstOrNull { it.id == AccessibilityNodeInfoCompat.ACTION_CLICK && it.label != null }?.label

    private companion object {
        const val CHIP_WIDTH = 300
    }

    private fun paymentMethod(type: PayPalSavedPaymentMethodType) = PayPalSavedPaymentMethod(
        type = type,
        label = "Visa",
        imageUrl = null,
        lastDigits = "1234",
        subtype = null
    )
}
