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
