@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodCreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PayPalCreditMessagingViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val sut = PayPalCreditMessagingView(context)
    private val shimmer: ShimmerPlaceholderView =
        sut.findViewById(R.id.paypal_saved_payment_method_credit_messaging_shimmer)
    private val text: TextView = sut.findViewById(R.id.paypal_saved_payment_method_credit_messaging_text)
    private val openedUrls = mutableListOf<String>()

    @Test
    fun `hidden hides the whole row`() {
        bind(PayPalCreditMessagingDisplayState.Hidden)

        assertFalse(sut.isVisible)
    }

    @Test
    fun `loading shows the shimmer only`() {
        bind(PayPalCreditMessagingDisplayState.Loading)

        assertTrue(sut.isVisible)
        assertTrue(shimmer.isVisible)
        assertFalse(text.isVisible)
    }

    @Test
    fun `a message with a link shows the link bold and opens its url when tapped`() {
        bind(PayPalCreditMessagingDisplayState.Content(CONTENT))

        assertFalse(shimmer.isVisible)
        assertEquals("Pay in 4. Learn more", text.text.toString())
        val spanned = text.text as Spanned
        val link = spanned.getSpans(0, spanned.length, ClickableSpan::class.java).single()
        assertEquals(10, spanned.getSpanStart(link))
        assertEquals(
            Typeface.BOLD,
            spanned.getSpans(spanned.getSpanStart(link), spanned.length, StyleSpan::class.java).single().style
        )
        link.onClick(text)
        assertEquals(listOf(URL), openedUrls)
    }

    @Test
    fun `the link uses the style's link color, otherwise the text color, and is underlined`() {
        bind(PayPalCreditMessagingDisplayState.Content(CONTENT), linkColor = Color.BLUE)
        assertEquals(Color.BLUE, drawnLink().color)
        assertTrue(drawnLink().isUnderlineText)

        bind(PayPalCreditMessagingDisplayState.Content(CONTENT))
        assertEquals(Color.RED, drawnLink().color)
    }

    @Test
    fun `a message without a link shows the message only`() {
        bind(PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4.", learnMore = null)))

        assertEquals("Pay in 4.", text.text.toString())
        assertEquals(0, (text.text as Spanned).getSpans(0, text.text.length, ClickableSpan::class.java).size)
    }

    private fun bind(state: PayPalCreditMessagingDisplayState, linkColor: Int? = null) {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = PayPalSavedPaymentMethodAppearance(textColor = Color.RED),
            container = PayPalSavedPaymentMethodContainerStyle(
                creditMessaging = PayPalSavedPaymentMethodCreditMessagingStyle(linkColor = linkColor)
            )
        )
        sut.bind(state, PayPalSavedPaymentMethodStyleResolver(context, style)) { openedUrls += it }
    }

    private fun drawnLink(): TextPaint {
        val spanned = text.text as Spanned
        val link = spanned.getSpans(0, spanned.length, ClickableSpan::class.java).single()
        return TextPaint().also { link.updateDrawState(it) }
    }

    private companion object {
        const val URL = "https://www.paypal.com/learn-more"
        val CONTENT = PayPalCreditMessagingContent(
            "Pay in 4.",
            PayPalCreditMessagingContent.LearnMore("Learn more", URL, isEmbeddable = false)
        )
    }
}
