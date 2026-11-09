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
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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
        bindMessage()

        assertFalse(shimmer.isVisible)
        assertEquals("Pay in 4. Learn more", text.text.toString())
        val spanned = text.text as Spanned
        val link = spanned.link()
        assertEquals(10, spanned.getSpanStart(link))
        assertEquals(
            Typeface.BOLD,
            spanned.getSpans(spanned.getSpanStart(link), spanned.length, StyleSpan::class.java).single().style
        )
        link.onClick(text)
        assertEquals(listOf(URL), openedUrls)
    }

    @Test
    @Config(sdk = [23])
    fun `screen readers can open the link before Android 8`() {
        bindMessage()

        // Before Android 8 only androidx's wrapper span is exposed to TalkBack, not the link's own ClickableSpan.
        val spanned = text.nodeInfo().text as Spanned
        val link = spanned.link()
        assertEquals("AccessibilityClickableSpanCompat", link.javaClass.simpleName)
        val linkText = spanned.subSequence(spanned.getSpanStart(link), spanned.getSpanEnd(link))
        assertEquals("Learn more", linkText.toString())
    }

    @Test
    fun `a TalkBack double tap on the message opens the link`() {
        bindMessage()

        assertTrue(text.isClickable)
        assertFalse(text.isLongClickable)
        assertEquals("Learn more", text.clickActionLabel())
        assertTrue(ViewCompat.performAccessibilityAction(text, AccessibilityNodeInfoCompat.ACTION_CLICK, null))
        assertEquals(listOf(URL), openedUrls)
    }

    @Test
    fun `a message without a link shows the message only and isn't a TalkBack double tap target`() {
        bindMessage()

        bindMessage(MESSAGE_ONLY)

        assertEquals("Pay in 4.", text.text.toString())
        assertEquals(0, (text.text as Spanned).getSpans(0, text.text.length, ClickableSpan::class.java).size)
        assertFalse(text.isClickable)
        assertNull(text.clickActionLabel())
        ViewCompat.performAccessibilityAction(text, AccessibilityNodeInfoCompat.ACTION_CLICK, null)
        assertTrue(openedUrls.isEmpty())
    }

    @Test
    fun `only a message with a link is a keyboard focus stop`() {
        bindMessage()
        assertTrue(text.isFocusable)

        bindMessage(MESSAGE_ONLY)
        assertFalse(text.isFocusable)

        bindMessage()
        assertTrue(text.isFocusable)
    }

    @Test
    fun `the link uses the style's link color without an underline`() {
        bindMessage(linkColor = Color.BLUE)

        assertEquals(Color.BLUE, drawnLink().color)
        assertFalse(drawnLink().isUnderlineText)
    }

    @Test
    fun `without a link color the link is underlined in the text color`() {
        bindMessage()

        assertEquals(Color.RED, drawnLink().color)
        assertTrue(drawnLink().isUnderlineText)
    }

    private fun bindMessage(content: PayPalCreditMessagingContent = CONTENT, linkColor: Int? = null) =
        bind(PayPalCreditMessagingDisplayState.Content(content), linkColor)

    private fun bind(state: PayPalCreditMessagingDisplayState, linkColor: Int? = null) {
        val style = PayPalSavedPaymentMethodViewStyle(
            componentAppearance = PayPalSavedPaymentMethodAppearance(textColor = Color.RED),
            container = PayPalSavedPaymentMethodContainerStyle(
                creditMessaging = PayPalSavedPaymentMethodCreditMessagingStyle(linkColor = linkColor)
            )
        )
        sut.bind(state, PayPalSavedPaymentMethodStyleResolver(context, style)) { openedUrls += it }
    }

    private fun Spanned.link(): ClickableSpan = getSpans(0, length, ClickableSpan::class.java).single()

    private fun drawnLink(): TextPaint = TextPaint().also { (text.text as Spanned).link().updateDrawState(it) }

    private companion object {
        const val URL = "https://www.paypal.com/learn-more"
        val CONTENT = PayPalCreditMessagingContent(
            "Pay in 4.",
            PayPalCreditMessagingContent.LearnMore("Learn more", URL, isEmbeddable = false)
        )
        val MESSAGE_ONLY = PayPalCreditMessagingContent("Pay in 4.", learnMore = null)
    }
}
