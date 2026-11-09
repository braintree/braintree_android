package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/** The Pay Later row: a shimmer while the message loads, then the message followed by its "Learn more" link. */
internal class PayPalCreditMessagingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val shimmerView: ShimmerPlaceholderView
    private val textView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.paypal_saved_payment_method_credit_messaging, this)
        shimmerView = findViewById(R.id.paypal_saved_payment_method_credit_messaging_shimmer)
        textView = findViewById(R.id.paypal_saved_payment_method_credit_messaging_text)
        textView.movementMethod = LinkMovementMethod.getInstance()
        // No highlight behind the link while it's tapped.
        textView.highlightColor = Color.TRANSPARENT
        // Before Android 8, TalkBack can't open a custom ClickableSpan such as the "Learn more" link without this.
        ViewCompat.enableAccessibleClickableSpanSupport(textView)
        // The movement method makes the text long-clickable, but a long press does nothing, so TalkBack shouldn't
        // offer it.
        textView.isLongClickable = false
    }

    /** Shows [state]; [PayPalCreditMessagingDisplayState.Hidden] hides the whole row. */
    fun bind(
        state: PayPalCreditMessagingDisplayState,
        resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
        onLearnMoreClick: (url: String) -> Unit
    ) {
        isVisible = state != PayPalCreditMessagingDisplayState.Hidden
        shimmerView.isVisible = state == PayPalCreditMessagingDisplayState.Loading
        textView.isVisible = state is PayPalCreditMessagingDisplayState.Content
        if (state is PayPalCreditMessagingDisplayState.Content) {
            textView.applyTextStyle(resolvedStyle, resolvedStyle.creditMessagingFontSizeSp)
            val linkColor = resolvedStyle.creditMessagingLinkColor
            textView.text = state.content.toSpannable(
                linkColor = linkColor ?: resolvedStyle.textColor,
                underline = linkColor == null,
                onLearnMoreClick = onLearnMoreClick
            )
            bindAccessibilityClick(state.content.learnMore, onLearnMoreClick)
        }
    }

    // TalkBack's double tap clicks the whole text, not the link, so the click opens "Learn more". A message without
    // the link isn't announced as something to double tap.
    private fun bindAccessibilityClick(
        learnMore: PayPalCreditMessagingContent.LearnMore?,
        onLearnMoreClick: (url: String) -> Unit
    ) {
        textView.isClickable = learnMore != null
        // The movement method made the text focusable; only a message with the link is a keyboard or D-pad stop.
        textView.isFocusable = learnMore != null
        if (learnMore != null) {
            ViewCompat.replaceAccessibilityAction(
                textView,
                AccessibilityActionCompat.ACTION_CLICK,
                learnMore.text
            ) { _, _ ->
                onLearnMoreClick(learnMore.url)
                true
            }
        } else {
            ViewCompat.removeAccessibilityAction(textView, AccessibilityNodeInfoCompat.ACTION_CLICK)
        }
    }
}

// The "Learn more" link is bold, and underlined only without a merchant link color, as on iOS.
private fun PayPalCreditMessagingContent.toSpannable(
    @ColorInt linkColor: Int,
    underline: Boolean,
    onLearnMoreClick: (String) -> Unit
): CharSequence {
    val builder = SpannableStringBuilder(message)
    val link = learnMore
    if (link != null) {
        builder.append(" ")
        val start = builder.length
        builder.append(link.text)
        val linkSpan = object : ClickableSpan() {
            override fun onClick(widget: View) = onLearnMoreClick(link.url)

            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = linkColor
                textPaint.isUnderlineText = underline
            }
        }
        builder.setSpan(linkSpan, start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return builder
}
