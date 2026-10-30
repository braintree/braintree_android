package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.LoaderImage
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.accessibilityLabel
import com.braintreepayments.api.paypalsavedpaymentmethod.state.iconRes
import com.braintreepayments.api.paypalsavedpaymentmethod.state.lastDigitsOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/**
 * Places [brand] and [fundingInstrument] side by side, [gap] apart, when the funding instrument fits at its full
 * width. Otherwise stacks them [stackedSpacing] apart.
 */
@Composable
internal fun SideBySideOrStacked(
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
            // Spacing only applies when the brand is shown.
            val spacing = if (brandPlaceable != null) stackedSpacing.roundToPx() else 0
            layout(maxOf(brandWidth, placeable.width), brandHeight + spacing + placeable.height) {
                brandPlaceable?.placeRelative(0, 0)
                placeable.placeRelative(0, brandHeight + spacing)
            }
        }
    }
}

/** The funding instrument or the buyer's email, with an edit control when the buyer can change it. */
@Composable
internal fun FundingInstrumentChip(
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
        // The whole chip is the edit target and is read as one element, e.g. "Visa, ending in 1 2 3 4".
        modifier = modifier
            // An editable chip gets a 48dp touch target, with the chip centered inside it.
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
                // Card art with a shimmer while it loads, and the generic icon if it fails to load.
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
                    // Shrinks a long label so the edit icon keeps its space.
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
private fun PayPalSavedPaymentMethod.displayText(): String =
    lastDigitsOrNull()?.let {
        stringResource(R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number, it)
    } ?: label
