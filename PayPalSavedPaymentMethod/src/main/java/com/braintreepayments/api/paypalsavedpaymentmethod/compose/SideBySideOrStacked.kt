package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

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
