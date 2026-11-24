package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/**
 * The Pay Later message with its "Learn more" link, or its shimmer while it loads. The shimmer is silent, since the
 * funding instrument shimmer already announces loading. Screen readers read the message and link as one element whose
 * action opens "Learn more", as in the XML view; otherwise the link alone is focusable and the message is read as part
 * of the whole card.
 */
@Composable
internal fun CreditMessagingRow(
    state: PayPalCreditMessagingDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    onLearnMoreClick: (PayPalCreditMessagingContent.LearnMore) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        // Wrapped: the placeholder replaces its own modifier's semantics, including the test tag.
        PayPalCreditMessagingDisplayState.Loading -> Box(modifier = modifier.fillMaxWidth()) {
            ShimmerPlaceholder(contentDescription = null)
        }
        is PayPalCreditMessagingDisplayState.Content -> {
            val textColor = Color(resolvedStyle.textColor)
            val linkColor = resolvedStyle.creditMessagingLinkColor?.let { Color(it) }
            val text = state.content.toAnnotatedString(linkColor, textColor, onLearnMoreClick)
            val learnMore = state.content.learnMore
            Text(
                text = text,
                color = textColor,
                fontSize = resolvedStyle.creditMessagingFontSizeSp.sp,
                fontFamily = resolvedStyle.fontFamily,
                modifier = modifier.clearAndSetSemantics {
                    this.text = text
                    learnMore?.let { onClick(label = it.text) { onLearnMoreClick(it); true } }
                }
            )
        }
        PayPalCreditMessagingDisplayState.Hidden -> Unit
    }
}

/** The message and the bold "Learn more" link, underlined in [textColor] when there's no merchant [linkColor]. */
private fun PayPalCreditMessagingContent.toAnnotatedString(
    linkColor: Color?,
    textColor: Color,
    onLearnMoreClick: (PayPalCreditMessagingContent.LearnMore) -> Unit
) =
    buildAnnotatedString {
        append(message)
        val learnMore = learnMore
        if (learnMore != null) {
            val text = learnMore.text
            append(" ")
            val linkStyle = SpanStyle(
                color = linkColor ?: textColor,
                fontWeight = FontWeight.Bold,
                textDecoration = if (linkColor == null) TextDecoration.Underline else TextDecoration.None
            )
            withLink(
                LinkAnnotation.Clickable(
                    tag = LEARN_MORE_LINK_TAG,
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onLearnMoreClick(learnMore) }
                )
            ) {
                append(text)
            }
        }
    }

private const val LEARN_MORE_LINK_TAG = "paypal_saved_payment_method_learn_more"
