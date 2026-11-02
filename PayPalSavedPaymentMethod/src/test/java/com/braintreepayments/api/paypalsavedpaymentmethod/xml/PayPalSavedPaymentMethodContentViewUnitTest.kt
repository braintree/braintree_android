@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
    fun `Pay Later lines up with the label past the logo, or at the leading edge without the logo`() {
        val style = PayPalSavedPaymentMethodViewStyle(
            container = PayPalSavedPaymentMethodContainerStyle(
                logo = PayPalLogoStyle(widthDp = 48f),
                label = PayPalLabelStyle(marginStartDp = 6f)
            )
        )
        render(CARD_STATE, MESSAGE_STATE, style = style)
        assertEquals(48f.dpToPx(context.resources), logo.layoutParams.width)
        assertEquals(48f.dpToPx(context.resources), logo.layoutParams.height)
        assertEquals(6f.dpToPx(context.resources), label.paddingStart)
        assertEquals(54f.dpToPx(context.resources), (creditMessaging.layoutParams as MarginLayoutParams).marginStart)

        render(CARD_STATE, MESSAGE_STATE, style = PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false))
        assertFalse(logo.isVisible)
        assertEquals(0, label.paddingStart)
        assertEquals(0, (creditMessaging.layoutParams as MarginLayoutParams).marginStart)
    }

    @Test
    fun `the logo is read as PayPal only when the label is hidden`() {
        render(CARD_STATE)
        assertNull(logo.contentDescription)

        render(CARD_STATE, style = PayPalSavedPaymentMethodViewStyle(showPayPalLabel = false))
        assertFalse(label.isVisible)
        assertEquals("PayPal", logo.contentDescription)
    }

    @Test
    fun `the merchant's height is a minimum`() {
        val style =
            PayPalSavedPaymentMethodViewStyle(container = PayPalSavedPaymentMethodContainerStyle(heightDp = 80f))
        render(CARD_STATE, style = style)

        assertEquals(80f.dpToPx(context.resources), sut.minimumHeight)
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
        val MESSAGE_STATE = PayPalCreditMessagingDisplayState.Content(
            PayPalCreditMessagingContent("Pay in 4.", learnMore = null)
        )
    }
}
