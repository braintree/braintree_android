@file:Suppress("TooManyFunctions", "MagicNumber", "UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.compose

import androidx.annotation.DimenRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.uicomponents.EditFiComponentStyle
import com.braintreepayments.api.uicomponents.EditFiDisplayState
import com.braintreepayments.api.uicomponents.FiSummary
import com.braintreepayments.api.uicomponents.FiType
import com.braintreepayments.api.uicomponents.R

/**
 * The presentational surface for the EditFiComponent — renders the sticky funding-instrument (FI)
 * "chip" (brand art + masked number + edit pencil) shown next to the PayPal label, plus loading
 * and fallback states (LLD §12, §13.2).
 *
 * Purely presentational: holds no PayPal client, performs no network calls, and drives everything
 * from [state]. The owning `EditFiComponent` (added in a later pass) fetches the FI, launches the
 * edit flow, and maps results into [state].
 *
 * The Pay Later credit-messaging row is a separate view (its own PR) and is not part of this
 * component; embedding the two together is a later pass.
 *
 * @param state      what to render — loading skeleton, an FI chip, the no-FI fallback, or the
 * add-card prompt.
 * @param modifier   Compose modifier for the outer container.
 * @param style      theming for the chip (see [EditFiComponentStyle]).
 * @param onEditClick invoked when the buyer taps the edit pencil.
 * @param onAddCardClick invoked when the buyer taps the "add a card" link in the [EditFiDisplayState.AddCard]
 * state. UI hook only — launching the add-card flow is wired in a later pass.
 */
@Composable
fun EditFiComponentView(
    state: EditFiDisplayState,
    modifier: Modifier = Modifier,
    style: EditFiComponentStyle = EditFiComponentStyle(),
    onEditClick: () -> Unit = {},
    onAddCardClick: () -> Unit = {},
) {
    Box(modifier = modifier) {
        when (state) {
            is EditFiDisplayState.Loading ->
                LoadingChip(style = style)

            is EditFiDisplayState.Content ->
                FiChip(fiSummary = state.fiSummary, style = style, onEditClick = onEditClick)

            is EditFiDisplayState.NoFi ->
                NoFiChip(buyerEmail = state.buyerEmail, style = style, onEditClick = onEditClick)

            is EditFiDisplayState.AddCard ->
                AddCardChip(style = style, onAddCardClick = onAddCardClick)
        }
    }
}

/**
 * The FI chip for a resolved instrument. Renders one of:
 * - card / bank: `[ brand art | fallback glyph ] ••last4 [ edit pencil ]`
 * - PayPal product: `[ PayPal logo ] <product name> [ edit pencil ]`
 * - Pay Later product: `<product name> [ edit pencil ]` (no icon)
 *
 * Long labels (e.g. "PayPal Cashback Mastercard") ellipsize at `edit_fi_label_max_width`.
 */
@Composable
private fun FiChip(fiSummary: FiSummary, style: EditFiComponentStyle, onEditClick: () -> Unit) {
    // PayPal products use the "Mark" box (45x30dp, monogram centered); card/bank art uses the
    // standard 28x20dp tile box.
    val isPayPalMark = fiSummary.type == FiType.PAYPAL
    val iconWidth = dimensionResource(
        if (isPayPalMark) R.dimen.edit_fi_paypal_mark_width else R.dimen.edit_fi_icon_width,
    )
    val iconHeight = dimensionResource(
        if (isPayPalMark) R.dimen.edit_fi_paypal_mark_height else R.dimen.edit_fi_icon_height,
    )
    val iconLabelSpacing = dimensionResource(R.dimen.edit_fi_icon_label_spacing)
    val labelTextSize = spDimensionResource(R.dimen.edit_fi_label_text_size)
    val labelMaxWidth = dimensionResource(R.dimen.edit_fi_label_max_width)
    val labelEditSpacing = dimensionResource(R.dimen.edit_fi_label_edit_spacing)

    Chip(style = style) {
        // Pay Later tiles (Pay in 4 / Pay Monthly) show the product name only — no icon.
        fiSummary.iconRes?.let { iconRes ->
            // Image + ContentScale.Fit so any art aspect (landscape card art, portrait PayPal
            // monogram) fits the icon box without distortion, centered within it.
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(iconWidth)
                    .height(iconHeight),
            )
            Spacer(modifier = Modifier.width(iconLabelSpacing))
        }
        Text(
            text = fiLabel(fiSummary),
            color = style.primaryTextColor,
            fontSize = labelTextSize,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = labelMaxWidth),
        )
        Spacer(modifier = Modifier.width(labelEditSpacing))
        EditButton(style = style, onClick = onEditClick)
    }
}

/** The no-FI fallback chip: "<buyer email> | Pay in Full" [ edit pencil ]. Fills width to ellipsize. */
@Composable
private fun NoFiChip(buyerEmail: String, style: EditFiComponentStyle, onEditClick: () -> Unit) {
    val labelTextSize = spDimensionResource(R.dimen.edit_fi_label_text_size)
    val labelEditSpacing = dimensionResource(R.dimen.edit_fi_label_edit_spacing)

    Chip(style = style, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                R.string.edit_fi_no_fi_format,
                buyerEmail,
                stringResource(R.string.edit_fi_pay_in_full),
            ),
            color = style.primaryTextColor,
            fontSize = labelTextSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(labelEditSpacing))
        EditButton(style = style, onClick = onEditClick)
    }
}

/**
 * The empty/disallowed-wallet chip: "⚠ To continue, add a card" on an amber background, where
 * "add a card" is styled as a link. The whole chip is the tap target ([onAddCardClick]).
 */
@Composable
private fun AddCardChip(style: EditFiComponentStyle, onAddCardClick: () -> Unit) {
    val cornerRadius = dimensionResource(R.dimen.edit_fi_chip_corner_radius)
    val paddingHorizontal = dimensionResource(R.dimen.edit_fi_chip_padding_horizontal)
    val paddingVertical = dimensionResource(R.dimen.edit_fi_chip_padding_vertical)
    val warningIconSize = dimensionResource(R.dimen.edit_fi_warning_icon_size)
    val iconLabelSpacing = dimensionResource(R.dimen.edit_fi_icon_label_spacing)
    val addCardTextSize = spDimensionResource(R.dimen.edit_fi_add_card_text_size)

    Surface(
        modifier = Modifier.clickable(onClick = onAddCardClick),
        shape = RoundedCornerShape(cornerRadius),
        color = style.addCardBackgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = paddingHorizontal,
                vertical = paddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.edit_fi_warning),
                contentDescription = null,
                tint = style.warningIconTint,
                modifier = Modifier.size(warningIconSize),
            )
            Spacer(modifier = Modifier.width(iconLabelSpacing))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.edit_fi_add_card_prefix))
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(stringResource(R.string.edit_fi_add_card_action))
                    }
                },
                color = style.primaryTextColor,
                fontSize = addCardTextSize,
                maxLines = 1,
            )
        }
    }
}

/** The loading skeleton chip. */
@Composable
private fun LoadingChip(style: EditFiComponentStyle) {
    val iconWidth = dimensionResource(R.dimen.edit_fi_icon_width)
    val iconHeight = dimensionResource(R.dimen.edit_fi_icon_height)
    val iconLabelSpacing = dimensionResource(R.dimen.edit_fi_icon_label_spacing)
    val labelWidth = dimensionResource(R.dimen.edit_fi_shimmer_label_width)
    val labelHeight = dimensionResource(R.dimen.edit_fi_shimmer_label_height)
    val shimmerShape = RoundedCornerShape(dimensionResource(R.dimen.edit_fi_shimmer_corner_radius))

    Chip(style = style) {
        ShimmerBox(
            modifier = Modifier
                .width(iconWidth)
                .height(iconHeight),
            shape = shimmerShape,
        )
        Spacer(modifier = Modifier.width(iconLabelSpacing))
        ShimmerBox(
            modifier = Modifier
                .width(labelWidth)
                .height(labelHeight),
            shape = shimmerShape,
        )
    }
}

/** The shared rounded chip container. */
@Composable
private fun Chip(style: EditFiComponentStyle, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val cornerRadius = dimensionResource(R.dimen.edit_fi_chip_corner_radius)
    val paddingHorizontal = dimensionResource(R.dimen.edit_fi_chip_padding_horizontal)
    val paddingVertical = dimensionResource(R.dimen.edit_fi_chip_padding_vertical)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = style.chipBackgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = paddingHorizontal,
                vertical = paddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun EditButton(style: EditFiComponentStyle, onClick: () -> Unit) {
    val touchTarget = dimensionResource(R.dimen.edit_fi_edit_touch_target)
    val iconSize = dimensionResource(R.dimen.edit_fi_edit_icon_size)

    Box(
        modifier = Modifier
            .size(touchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.edit_fi_edit_pencil),
            contentDescription = stringResource(R.string.edit_fi_edit_content_description),
            tint = style.editIconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The chip label: the product [FiSummary.displayName] for PayPal / Pay Later products, otherwise
 * the masked number (e.g. "••3339"). Empty when neither is available.
 */
@Composable
private fun fiLabel(fiSummary: FiSummary): String =
    fiSummary.displayName ?: fiSummary.last4?.let {
        stringResource(R.string.edit_fi_masked_number, it)
    } ?: ""

/**
 * Reads an `sp` font-size dimension from resources (Compose has no direct `sp` equivalent of
 * [dimensionResource], which returns a `Dp`). Reading the value as a `Dp`
 * and converting it back with `Density.toSp` yields the declared `sp` size
 * while keeping the dimension in `res/values/dimens.xml` per the module convention (dp/sp live in
 * dimens.xml, colors in colors.xml — see PayPalButtonView / CardFields).
 */
@Composable
private fun spDimensionResource(@DimenRes id: Int): TextUnit =
    with(LocalDensity.current) { dimensionResource(id).toSp() }

// region Previews
// Preview mock data — for design review only; not used at runtime.

/**
 * Gallery of every FI tag version from the design spec, stacked to mirror the "Tag Versions"
 * column. Design-review only.
 */
@Preview(name = "All tag versions", showBackground = true, widthDp = 360)
@Composable
private fun PreviewEditFiAllTagVersions() {
    Column(modifier = Modifier.padding(16.dp)) {
        val tags = listOf(
            EditFiDisplayState.Loading,
            EditFiDisplayState.Content(FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD)),
            EditFiDisplayState.Content(FiSummary(brand = "Mastercard", last4 = "3434", type = FiType.CARD)),
            EditFiDisplayState.Content(FiSummary(type = FiType.CARD, displayName = "PayPal Cashback Mastercard")),
            EditFiDisplayState.Content(FiSummary(type = FiType.PAYPAL, displayName = "PayPal Credit")),
            EditFiDisplayState.Content(FiSummary(type = FiType.PAYPAL, displayName = "PayPal Credit Card")),
            EditFiDisplayState.Content(FiSummary(type = FiType.PAY_LATER, displayName = "Pay in 4")),
            EditFiDisplayState.Content(FiSummary(type = FiType.PAY_LATER, displayName = "Pay Monthly")),
            EditFiDisplayState.Content(FiSummary(brand = null, last4 = "3339", type = FiType.CARD)),
            EditFiDisplayState.Content(FiSummary(last4 = "3339", type = FiType.BANK)),
            EditFiDisplayState.NoFi(buyerEmail = "alex.burgos@gmail.com"),
            EditFiDisplayState.NoFi(buyerEmail = "a.very.long.buyer.email.address@somelongdomainname.example.com"),
            EditFiDisplayState.AddCard,
        )
        tags.forEach { tag ->
            EditFiComponentView(state = tag)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Recreates the View/Edit FI design in context: PayPal label → FI chip. The PayPal logo/label
 * chrome here is PREVIEW-ONLY (it belongs to the merchant's payment sheet); the component itself is
 * only the FI chip.
 */
@Preview(name = "In context (PayPal row)", showBackground = true, widthDp = 420)
@Composable
private fun PreviewEditFiInContext() {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.paypal_monogram),
            contentDescription = "PayPal",
            tint = Color.Unspecified,
            modifier = Modifier.height(24.dp).width(21.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "PayPal", color = Color(0xFF111111), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        EditFiComponentView(
            state = EditFiDisplayState.Content(FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD)),
        )
    }
}

// endregion
