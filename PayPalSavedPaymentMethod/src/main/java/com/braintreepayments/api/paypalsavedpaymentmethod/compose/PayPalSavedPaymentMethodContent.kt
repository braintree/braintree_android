package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.LoaderImage
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.accessibilityLabel
import com.braintreepayments.api.paypalsavedpaymentmethod.state.iconRes
import com.braintreepayments.api.paypalsavedpaymentmethod.state.lastDigitsOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.state.showsFundingInstrumentRow
import com.braintreepayments.api.paypalsavedpaymentmethod.state.visibleWith
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

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
    val showsBrandMark = resolvedStyle.showPayPalLogo || resolvedStyle.showPayPalLabel
    val fundingInstrumentGap = resolvedStyle.fundingInstrumentMarginStartDp.dp

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
            val brandMark: @Composable () -> Unit = { PayPalBrandMark(resolvedStyle, labelGap) }
            if (fundingInstrumentState == PayPalSavedPaymentMethodDisplayState.Loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    brandMark()
                    ShimmerPlaceholder(
                        contentDescription =
                            stringResource(R.string.paypal_saved_payment_method_loading_content_description),
                        modifier = Modifier
                            .weight(1f)
                            // The gap only separates the funding instrument from the PayPal logo and label.
                            .padding(start = if (showsBrandMark) fundingInstrumentGap else 0.dp)
                    )
                }
            } else {
                SideBySideOrStacked(
                    gap = fundingInstrumentGap,
                    stackedSpacing = dimensionResource(R.dimen.paypal_saved_payment_method_stacked_layout_spacing),
                    brand = brandMark,
                    fundingInstrument = {
                        if (showFundingInstrument) {
                            FundingInstrumentChip(
                                state = fundingInstrumentState,
                                resolvedStyle = resolvedStyle,
                                editEnabled = editEnabled,
                                onEditClick = onEditClick
                            )
                        }
                    }
                )
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

/** The PayPal logo and label, either of which the merchant can hide. */
@Composable
private fun PayPalBrandMark(resolvedStyle: PayPalSavedPaymentMethodStyleResolver, labelGap: Dp) {
    if (!resolvedStyle.showPayPalLogo && !resolvedStyle.showPayPalLabel) return
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
    }
}

/**
 * Places [brand] and [fundingInstrument] side by side, [gap] apart and centered vertically, when the funding instrument
 * fits at its full width. Otherwise, for example at large text sizes, it stacks them [stackedSpacing] apart, so the
 * funding instrument stays legible instead of being cut off.
 */
@Composable
private fun SideBySideOrStacked(
    gap: Dp,
    stackedSpacing: Dp,
    brand: @Composable () -> Unit,
    fundingInstrument: @Composable () -> Unit
) {
    Layout(contents = listOf(brand, fundingInstrument)) { measurables, constraints ->
        val (brandMeasurables, fundingInstrumentMeasurables) = measurables
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val brandPlaceable = brandMeasurables.firstOrNull()?.measure(looseConstraints)
        val brandWidth = brandPlaceable?.width ?: 0
        val brandHeight = brandPlaceable?.height ?: 0
        val fundingInstrumentMeasurable = fundingInstrumentMeasurables.firstOrNull()
            ?: return@Layout layout(brandWidth, brandHeight) { brandPlaceable?.place(0, 0) }

        val gapPx = if (brandPlaceable != null) gap.roundToPx() else 0
        val fullWidth = fundingInstrumentMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val fitsSideBySide = !constraints.hasBoundedWidth || brandWidth + gapPx + fullWidth <= constraints.maxWidth
        if (fitsSideBySide) {
            val maxWidth = if (constraints.hasBoundedWidth) {
                (constraints.maxWidth - brandWidth - gapPx).coerceAtLeast(0)
            } else {
                Constraints.Infinity
            }
            val placeable = fundingInstrumentMeasurable.measure(looseConstraints.copy(maxWidth = maxWidth))
            val height = maxOf(brandHeight, placeable.height)
            layout(brandWidth + gapPx + placeable.width, height) {
                brandPlaceable?.placeRelative(0, (height - brandHeight) / 2)
                placeable.placeRelative(brandWidth + gapPx, (height - placeable.height) / 2)
            }
        } else {
            val placeable = fundingInstrumentMeasurable.measure(looseConstraints)
            // The spacing only separates the funding instrument from the PayPal logo and label.
            val spacing = if (brandPlaceable != null) stackedSpacing.roundToPx() else 0
            layout(maxOf(brandWidth, placeable.width), brandHeight + spacing + placeable.height) {
                brandPlaceable?.placeRelative(0, 0)
                placeable.placeRelative(0, brandHeight + spacing)
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
    val brandName = if (resolvedStyle.showPayPalLogo || resolvedStyle.showPayPalLabel) {
        null
    } else {
        stringResource(R.string.paypal_saved_payment_method_label)
    }
    val accessibilityLabel = state.accessibilityLabel(brandName) { label, spokenDigits ->
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
            // A chip the buyer can tap gets the minimum 48dp touch target, with the chip centered inside it.
            .then(if (isEditable) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(chipShape)
            .background(colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_background))
            .semantics { accessibilityLabel?.let { contentDescription = it } }
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
                val fallbackIcon = paymentMethod.iconRes()
                // Card art from PayPal, with a shimmer while it loads and the generic card or bank icon if it can't be
                // loaded.
                if (paymentMethod.imageUrl != null || fallbackIcon != null) {
                    val iconWidth = dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_width)
                    val iconLoadingHeight =
                        dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_loading_height)
                    LoaderImage(
                        url = paymentMethod.imageUrl,
                        placeholder = fallbackIcon?.let { painterResource(it) } ?: ColorPainter(Color.Transparent),
                        modifier = Modifier
                            .padding(end = iconMargin)
                            .width(iconWidth),
                        loading = {
                            // Decorative: the chip is read as one element.
                            ShimmerPlaceholder(
                                contentDescription = null,
                                modifier = it,
                                height = iconLoadingHeight,
                                trailingGap = 0.dp
                            )
                        }
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
        color = colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_text),
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
            val linkColor = resolvedStyle.creditMessagingLinkColor?.let { Color(it) }
            Text(
                text = state.content.toAnnotatedString(
                    linkColor = linkColor ?: textColor,
                    underline = linkColor == null,
                    onLearnMoreClick = onLearnMoreClick
                ),
                color = textColor,
                fontSize = resolvedStyle.creditMessagingFontSizeSp.sp,
                fontFamily = resolvedStyle.fontFamily,
                modifier = modifier
            )
        }
        PayPalCreditMessagingDisplayState.Hidden -> Unit
    }
}

// The "Learn more" link is bold, and underlined only without a merchant link color, as on iOS.
private fun PayPalCreditMessagingContent.toAnnotatedString(
    linkColor: Color,
    underline: Boolean,
    onLearnMoreClick: (String) -> Unit
) =
    buildAnnotatedString {
        append(message)
        val link = learnMore
        if (link != null) {
            append(" ")
            val linkStyle = SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.Bold,
                textDecoration = if (underline) TextDecoration.Underline else null
            )
            withLink(
                LinkAnnotation.Clickable(
                    tag = LEARN_MORE_LINK_TAG,
                    styles = TextLinkStyles(style = linkStyle),
                    linkInteractionListener = { onLearnMoreClick(link.url) }
                )
            ) {
                append(link.text)
            }
        }
    }

@Composable
private fun PayPalSavedPaymentMethod.displayText(): String =
    lastDigitsOrNull()?.let {
        stringResource(R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number, it)
    } ?: label

private val PayPalSavedPaymentMethodStyleResolver.fontFamily: FontFamily
    get() = fontResId?.let { FontFamily(Font(it)) } ?: FontFamily.Default

private const val LEARN_MORE_LINK_TAG = "paypal_saved_payment_method_learn_more"
