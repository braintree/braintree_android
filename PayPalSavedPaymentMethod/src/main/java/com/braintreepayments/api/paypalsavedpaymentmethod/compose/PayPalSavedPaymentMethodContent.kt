package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.accessibilityLabel
import com.braintreepayments.api.paypalsavedpaymentmethod.iconRes
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.LoaderImage
import com.braintreepayments.api.paypalsavedpaymentmethod.isEditable
import com.braintreepayments.api.paypalsavedpaymentmethod.maskedLastDigitsOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.showsFundingInstrumentRow
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.visibleWith

/**
 * Renders the component from its two display states: the PayPal logo and label, the funding instrument chip with its
 * edit control, and the Pay Later message below. Holds no state and makes no network calls.
 */
@OptIn(ExperimentalBetaApi::class)
@Composable
internal fun PayPalSavedPaymentMethodContent(
    fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
    creditMessagingState: PayPalCreditMessagingDisplayState,
    style: PayPalSavedPaymentMethodViewStyle,
    onEditClick: () -> Unit,
    onLearnMoreClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    editEnabled: Boolean = true,
    isEditCompleted: Boolean = false
) {
    if (fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Hidden) return

    val context = LocalContext.current
    val resolvedStyle = remember(style, context) { PayPalSavedPaymentMethodStyleResolver(context, style) }
    val showFundingInstrument = fundingInstrumentState.showsFundingInstrumentRow
    val visibleCreditMessagingState = creditMessagingState.visibleWith(fundingInstrumentState, isEditCompleted)
    val showCreditMessaging = resolvedStyle.showPayPalCreditMessaging &&
        visibleCreditMessagingState != PayPalCreditMessagingDisplayState.Hidden
    val labelGap = resolvedStyle.labelMarginStartDp.dp

    Card(
        // The merchant's height is a minimum, so larger text never gets clipped.
        modifier = modifier.heightIn(min = resolvedStyle.heightDp?.dp ?: 0.dp),
        shape = RoundedCornerShape(resolvedStyle.cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = Color(resolvedStyle.backgroundColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (resolvedStyle.borderWidthDp > 0f) {
            BorderStroke(resolvedStyle.borderWidthDp.dp, Color(resolvedStyle.borderColor))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = resolvedStyle.horizontalPaddingDp.dp,
                    vertical = resolvedStyle.verticalPaddingDp.dp
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (resolvedStyle.showPayPalLogo) {
                    Image(
                        painter = painterResource(R.drawable.ic_paypal_brand_logo),
                        // The label already reads "PayPal" when it's shown.
                        contentDescription = if (resolvedStyle.showPayPalLabel) {
                            null
                        } else {
                            stringResource(R.string.paypal_saved_payment_method_label)
                        },
                        // A square box; the artwork scales to fit inside it, keeping its aspect ratio.
                        modifier = Modifier.size(resolvedStyle.logoWidthDp.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (resolvedStyle.showPayPalLabel) {
                    Text(
                        text = stringResource(R.string.paypal_saved_payment_method_label),
                        color = Color(resolvedStyle.textColor),
                        fontSize = resolvedStyle.labelFontSizeSp.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = resolvedStyle.fontFamily,
                        // The gap only separates the label from the logo.
                        modifier = Modifier.padding(start = if (resolvedStyle.showPayPalLogo) labelGap else 0.dp)
                    )
                }
                if (fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Loading) {
                    ShimmerPlaceholder(
                        contentDescription =
                            stringResource(R.string.paypal_saved_payment_method_loading_content_description),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = resolvedStyle.fundingInstrumentMarginStartDp.dp)
                    )
                } else if (showFundingInstrument) {
                    FundingInstrumentChip(
                        state = fundingInstrumentState,
                        resolvedStyle = resolvedStyle,
                        editEnabled = editEnabled,
                        onEditClick = onEditClick,
                        modifier = Modifier.padding(start = resolvedStyle.fundingInstrumentMarginStartDp.dp)
                    )
                }
            }
            if (showCreditMessaging) {
                CreditMessagingRow(
                    state = visibleCreditMessagingState,
                    resolvedStyle = resolvedStyle,
                    onLearnMoreClick = onLearnMoreClick,
                    modifier = Modifier.padding(
                        // Lined up with the label: past the logo when it's shown, otherwise at the leading edge.
                        start = if (resolvedStyle.showPayPalLogo) resolvedStyle.logoWidthDp.dp + labelGap else 0.dp,
                        top = dimensionResource(R.dimen.paypal_saved_payment_method_credit_messaging_top_spacing)
                    )
                )
            }
        }
    }
}

@Composable
private fun FundingInstrumentChip(
    state: PayPalSavedPaymentMethodDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    editEnabled: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconMargin = dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_margin)
    val editActionLabel = stringResource(R.string.paypal_saved_payment_method_edit_action_label)
    val context = LocalContext.current
    val accessibilityLabel = state.accessibilityLabel { label, spokenDigits ->
        context.getString(R.string.paypal_saved_payment_method_funding_instrument_ending_in, label, spokenDigits)
    }
    val chipShape = RoundedCornerShape(
        dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_chip_corner_radius)
    )
    val isEditable = state.isEditable
    Row(
        // The whole chip is the edit target, so it's larger than the edit icon alone. A buyer who can't edit gets a
        // display-only chip. Screen readers read the chip as one element, for example "Visa, ending in 1 2 3 4".
        modifier = modifier
            .clip(chipShape)
            .background(colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_background))
            .semantics { contentDescription = accessibilityLabel }
            .then(
                if (isEditable) {
                    Modifier.clickable(
                        enabled = editEnabled,
                        onClickLabel = editActionLabel,
                        role = Role.Button,
                        onClick = onEditClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = dimensionResource(
                    R.dimen.paypal_saved_payment_method_funding_instrument_chip_horizontal_padding
                ),
                vertical = dimensionResource(
                    R.dimen.paypal_saved_payment_method_funding_instrument_chip_vertical_padding
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            is PayPalSavedPaymentMethodDisplayState.Content -> {
                val paymentMethod = state.paymentMethod
                val fallbackIcon = paymentMethod.type.iconRes()
                // Card art from PayPal, with the generic card or bank icon until it loads or if it can't be loaded.
                if (paymentMethod.imageUrl != null || fallbackIcon != null) {
                    LoaderImage(
                        url = paymentMethod.imageUrl,
                        placeholder = fallbackIcon?.let { painterResource(it) } ?: ColorPainter(Color.Transparent),
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_width))
                            .padding(end = iconMargin)
                    )
                }
                FundingInstrumentText(
                    text = paymentMethod.displayText(),
                    resolvedStyle = resolvedStyle,
                    // Shrinks a long label so the edit icon after it keeps its space.
                    modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics { }
                )
            }
            is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument ->
                FundingInstrumentText(
                    text = state.payerEmail,
                    resolvedStyle = resolvedStyle,
                    modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics { }
                )
            else -> Unit
        }
        if (isEditable) {
            // Decorative: the chip carries the edit action.
            Image(
                painter = painterResource(R.drawable.ic_edit_pencil),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = iconMargin)
                    .size(resolvedStyle.editIconSizeDp.dp)
            )
        }
    }
}

@Composable
private fun FundingInstrumentText(
    text: String,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = Color(resolvedStyle.textColor),
        fontSize = resolvedStyle.fundingInstrumentTextFontSizeSp.sp,
        fontFamily = resolvedStyle.fontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CreditMessagingRow(
    state: PayPalCreditMessagingDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    onLearnMoreClick: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        // Decorative only: the funding instrument skeleton already announces the loading state.
        PayPalCreditMessagingDisplayState.Loading -> ShimmerPlaceholder(
            contentDescription = null,
            modifier = modifier.fillMaxWidth()
        )
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

/** The message followed by the "Learn more" link, shown bold and underlined so it stands out in any color. */
private fun PayPalCreditMessagingContent.toAnnotatedString(linkColor: Color, onLearnMoreClick: (String) -> Unit) =
    buildAnnotatedString {
        append(message)
        val text = learnMoreText
        val url = learnMoreUrl
        if (text != null && url != null) {
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

@Composable
private fun PayPalSavedPaymentMethod.displayText(): String =
    maskedLastDigitsOrNull()?.let {
        stringResource(R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number, it)
    } ?: label

private val PayPalSavedPaymentMethodStyleResolver.fontFamily: FontFamily
    get() = fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default

private const val LEARN_MORE_LINK_TAG = "paypal_saved_payment_method_learn_more"
