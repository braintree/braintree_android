@file:Suppress("TooManyFunctions", "MagicNumber", "UnusedPrivateMember")

package com.braintreepayments.api.uicomponents.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    Chip(style = style) {
        // Pay Later tiles (Pay in 4 / Pay Monthly) show the product name only — no icon.
        fiSummary.iconRes?.let { iconRes ->
            // Image + ContentScale.Fit so any art aspect (landscape card art, portrait PayPal
            // monogram) fits the fixed icon box without distortion.
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(dimensionResource(R.dimen.edit_fi_icon_width))
                    .height(dimensionResource(R.dimen.edit_fi_icon_height)),
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.edit_fi_icon_label_spacing)))
        }
        Text(
            text = fiLabel(fiSummary),
            color = style.primaryTextColor,
            fontSize = LABEL_TEXT_SIZE,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = dimensionResource(R.dimen.edit_fi_label_max_width)),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.edit_fi_label_edit_spacing)))
        EditButton(style = style, onClick = onEditClick)
    }
}

/** The no-FI fallback chip: "<buyer email> | Pay in Full" [ edit pencil ]. Fills width to ellipsize. */
@Composable
private fun NoFiChip(buyerEmail: String, style: EditFiComponentStyle, onEditClick: () -> Unit) {
    Chip(style = style, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                R.string.edit_fi_no_fi_format,
                buyerEmail,
                stringResource(R.string.edit_fi_pay_in_full),
            ),
            color = style.primaryTextColor,
            fontSize = LABEL_TEXT_SIZE,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.edit_fi_label_edit_spacing)))
        EditButton(style = style, onClick = onEditClick)
    }
}

/**
 * The empty/disallowed-wallet chip: "⚠ To continue, add a card" on an amber background, where
 * "add a card" is styled as a link. The whole chip is the tap target ([onAddCardClick]).
 */
@Composable
private fun AddCardChip(style: EditFiComponentStyle, onAddCardClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onAddCardClick),
        shape = RoundedCornerShape(dimensionResource(R.dimen.edit_fi_chip_corner_radius)),
        color = style.addCardBackgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.edit_fi_chip_padding_horizontal),
                vertical = dimensionResource(R.dimen.edit_fi_chip_padding_vertical),
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.edit_fi_warning),
                contentDescription = null,
                tint = style.warningIconTint,
                modifier = Modifier.size(dimensionResource(R.dimen.edit_fi_warning_icon_size)),
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.edit_fi_icon_label_spacing)))
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
                fontSize = ADD_CARD_TEXT_SIZE,
                maxLines = 1,
            )
        }
    }
}

/** The loading skeleton chip. */
@Composable
private fun LoadingChip(style: EditFiComponentStyle) {
    Chip(style = style) {
        ShimmerBox(
            modifier = Modifier
                .width(dimensionResource(R.dimen.edit_fi_icon_width))
                .height(dimensionResource(R.dimen.edit_fi_icon_height)),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.edit_fi_icon_label_spacing)))
        ShimmerBox(
            modifier = Modifier
                .width(dimensionResource(R.dimen.edit_fi_shimmer_label_width))
                .height(dimensionResource(R.dimen.edit_fi_shimmer_label_height)),
        )
    }
}

/** The shared rounded chip container. */
@Composable
private fun Chip(style: EditFiComponentStyle, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.edit_fi_chip_corner_radius)),
        color = style.chipBackgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.edit_fi_chip_padding_horizontal),
                vertical = dimensionResource(R.dimen.edit_fi_chip_padding_vertical),
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun EditButton(style: EditFiComponentStyle, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.edit_fi_edit_touch_target))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.edit_fi_edit_pencil),
            contentDescription = stringResource(R.string.edit_fi_edit_content_description),
            tint = style.editIconTint,
            modifier = Modifier.size(dimensionResource(R.dimen.edit_fi_edit_icon_size)),
        )
    }
}

/** A simple pulsing skeleton block used by [LoadingChip]. */
@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "edit-fi-shimmer")
    val alpha by transition.animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "edit-fi-shimmer-alpha",
    )
    Spacer(
        modifier = modifier.background(
            color = SHIMMER_COLOR.copy(alpha = alpha),
            shape = RoundedCornerShape(dimensionResource(R.dimen.edit_fi_shimmer_corner_radius)),
        ),
    )
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

// dp/sp layout dimensions live in res/values/dimens.xml (edit_fi_*) — read via dimensionResource,
// matching the module convention (see PayPalButtonView / CardFields). Font sizes stay here as sp
// constants since there is no Compose sp-from-resource equivalent for fontSize.
private val LABEL_TEXT_SIZE = 14.sp
private val ADD_CARD_TEXT_SIZE = 14.sp
private val SHIMMER_COLOR = Color(0xFFE4E7EC)
private const val SHIMMER_MIN_ALPHA = 0.3f
private const val SHIMMER_MAX_ALPHA = 0.9f
private const val SHIMMER_DURATION_MS = 800

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
