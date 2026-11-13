package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Places [brand] and [fundingInstrument] side by side, [gap] apart, when the funding instrument fits at its full width
 * or [canStack] is `false`. Otherwise stacks them [stackedSpacing] apart. [below] goes under both, [belowSpacing]
 * apart, starting at [belowIndent], or lined up with the funding instrument when [belowIndent] is `null`.
 */
@Composable
internal fun FundingInstrumentLayout(
    gap: Dp,
    stackedSpacing: Dp,
    brand: @Composable () -> Unit,
    fundingInstrument: @Composable () -> Unit,
    below: @Composable () -> Unit = {},
    belowSpacing: Dp = 0.dp,
    belowIndent: Dp? = 0.dp,
    canStack: Boolean = true
) {
    Layout(contents = listOf(brand, fundingInstrument, below)) { measurables, constraints ->
        val (brandMeasurables, fundingInstrumentMeasurables, belowMeasurables) = measurables
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val brandPlaceable = brandMeasurables.firstOrNull()?.measure(looseConstraints)
        val row = measureRow(
            brand = brandPlaceable,
            fundingInstrument = fundingInstrumentMeasurables.firstOrNull(),
            gapPx = if (brandPlaceable == null) 0 else gap.roundToPx(),
            stackedSpacingPx = stackedSpacing.roundToPx(),
            constraints = looseConstraints,
            canStack = canStack
        )
        val belowX = belowIndent?.roundToPx() ?: row.fundingInstrumentX
        val belowPlaceable = belowMeasurables.firstOrNull()?.measure(
            looseConstraints.copy(maxWidth = looseConstraints.widthFrom(belowX))
        )
        val belowY = row.height + if (belowPlaceable == null) 0 else belowSpacing.roundToPx()
        layout(maxOf(row.width, belowX + belowPlaceable.widthOrZero), belowY + belowPlaceable.heightOrZero) {
            with(row) { place() }
            belowPlaceable?.placeRelative(belowX, belowY)
        }
    }
}

/**
 * Measures the brand and funding instrument row: side by side if the funding instrument fits at its full width or
 * [canStack] is `false`, otherwise stacked, with the spacing only when the brand is shown.
 */
private fun measureRow(
    brand: Placeable?,
    fundingInstrument: Measurable?,
    gapPx: Int,
    stackedSpacingPx: Int,
    constraints: Constraints,
    canStack: Boolean
): MeasuredRow {
    val fullWidth = fundingInstrument?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
    val isStacked = canStack && fundingInstrument != null && constraints.hasBoundedWidth &&
        brand.widthOrZero + gapPx + fullWidth > constraints.maxWidth
    val fundingInstrumentX = if (isStacked) 0 else brand.widthOrZero + gapPx
    val fundingInstrumentConstraints = constraints.copy(maxWidth = constraints.widthFrom(fundingInstrumentX))
    return MeasuredRow(
        brand = brand,
        fundingInstrument = fundingInstrument?.measure(fundingInstrumentConstraints),
        isStacked = isStacked,
        fundingInstrumentX = fundingInstrumentX,
        spacingPx = if (isStacked && brand != null) stackedSpacingPx else 0
    )
}

private class MeasuredRow(
    val brand: Placeable?,
    val fundingInstrument: Placeable?,
    val isStacked: Boolean,
    val fundingInstrumentX: Int,
    val spacingPx: Int
) {
    val height = if (isStacked) {
        brand.heightOrZero + spacingPx + fundingInstrument.heightOrZero
    } else {
        maxOf(brand.heightOrZero, fundingInstrument.heightOrZero)
    }
    val width = maxOf(brand.widthOrZero, fundingInstrumentX + fundingInstrument.widthOrZero)

    fun Placeable.PlacementScope.place() {
        brand?.placeRelative(0, if (isStacked) 0 else (height - brand.height) / 2)
        fundingInstrument?.placeRelative(
            fundingInstrumentX,
            if (isStacked) brand.heightOrZero + spacingPx else (height - fundingInstrument.height) / 2
        )
    }
}

private val Placeable?.widthOrZero: Int get() = this?.width ?: 0
private val Placeable?.heightOrZero: Int get() = this?.height ?: 0

/** The width left after [start], or unbounded. */
private fun Constraints.widthFrom(start: Int): Int =
    if (hasBoundedWidth) (maxWidth - start).coerceAtLeast(0) else Constraints.Infinity
