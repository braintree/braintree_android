package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** The Pay Later message with its "Learn more" link, or its shimmer while it loads. */
@Composable
internal fun CreditMessagingRow(
    state: PayPalCreditMessagingDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    onLearnMoreClick: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        // Decorative: the funding instrument shimmer already announces loading.
        // Wrapped, since the placeholder replaces the semantics of its own modifier, including the test tag.
        PayPalCreditMessagingDisplayState.Loading -> Box(modifier = modifier.fillMaxWidth()) {
            ShimmerPlaceholder(contentDescription = null)
        }
        is PayPalCreditMessagingDisplayState.Content -> {
            val textColor = Color(resolvedStyle.textColor)
            val linkColor = resolvedStyle.creditMessagingLinkColor?.let { Color(it) } ?: textColor
            Text(
                text = state.content.toAnnotatedString(linkColor, onLearnMoreClick),
                color = textColor,
                fontSize = resolvedStyle.creditMessagingFontSizeSp.sp,
                fontFamily = resolvedStyle.fontFamily,
                modifier = modifier
            )
        }
        PayPalCreditMessagingDisplayState.Hidden -> Unit
    }
}

/** The message followed by the "Learn more" link, bold and underlined. */
private fun PayPalCreditMessagingContent.toAnnotatedString(linkColor: Color, onLearnMoreClick: (String) -> Unit) =
    buildAnnotatedString {
        append(message)
        val learnMore = learnMore
        if (learnMore != null) {
            val text = learnMore.text
            val url = learnMore.url
            append(" ")
            val linkStyle = SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
            withLink(
                LinkAnnotation.Clickable(
                    tag = LEARN_MORE_LINK_TAG,
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onLearnMoreClick(url) }
                )
            ) {
                append(text)
            }
        }
    }

private const val LEARN_MORE_LINK_TAG = "paypal_saved_payment_method_learn_more"
