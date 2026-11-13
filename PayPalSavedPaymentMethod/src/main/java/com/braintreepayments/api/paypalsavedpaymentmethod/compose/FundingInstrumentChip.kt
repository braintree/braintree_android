package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.ImageLoader
import com.braintreepayments.api.paypalsavedpaymentmethod.imageloader.LoaderImage
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.accessibilityLabel
import com.braintreepayments.api.paypalsavedpaymentmethod.state.iconRes
import com.braintreepayments.api.paypalsavedpaymentmethod.state.lastDigitsOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

/**
 * The funding instrument or the buyer's email, with an edit control when the buyer can change it. The whole chip is
 * the edit target and screen readers read it as one element, for example "Visa, ending in 1 2 3 4", so its icons and
 * text are decorative. An editable chip gets a 48dp touch target and a fixed dark ripple, since the theme's ripple is
 * light on a dark theme and barely shows on the light chip. Card art shows a shimmer while it loads and the generic
 * icon if it fails; with no generic icon it leaves no space, as on iOS. The art's frame is capped, so square or
 * portrait art scales down instead of growing the chip, and a long label shrinks so the edit icon keeps its space.
 */
@Composable
internal fun FundingInstrumentChip(
    state: PayPalSavedPaymentMethodDisplayState,
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    editEnabled: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = remember { ImageLoader.getInstance() }
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
        modifier = modifier
            .then(if (isEditable) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(chipShape)
            .background(colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_background))
            .semantics { accessibilityLabel?.let { contentDescription = it } }
            .then(
                if (isEditable) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(
                            color = colorResource(R.color.paypal_saved_payment_method_funding_instrument_chip_ripple)
                        ),
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
                if (paymentMethod.imageUrl != null || fallbackIcon != null) {
                    val iconWidth = dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_width)
                    val iconHeight =
                        dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_height)
                    val iconLoadingHeight =
                        dimensionResource(R.dimen.paypal_saved_payment_method_funding_instrument_icon_loading_height)
                    LoaderImage(
                        url = paymentMethod.imageUrl,
                        placeholder = fallbackIcon?.let { painterResource(it) },
                        modifier = Modifier
                            .padding(end = iconMargin)
                            .width(iconWidth)
                            .heightIn(max = iconHeight),
                        loading = {
                            ShimmerPlaceholder(
                                contentDescription = null,
                                modifier = it,
                                height = iconLoadingHeight,
                                trailingGap = 0.dp
                            )
                        },
                        imageLoader = imageLoader
                    )
                }
                FundingInstrumentText(
                    text = paymentMethod.displayText(),
                    resolvedStyle = resolvedStyle,
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

/** The chip's text, in a fixed color as on iOS, so it stays readable on the chip's fixed background. */
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
private fun PayPalSavedPaymentMethod.displayText(): String =
    lastDigitsOrNull()?.let {
        stringResource(R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number, it)
    } ?: label
