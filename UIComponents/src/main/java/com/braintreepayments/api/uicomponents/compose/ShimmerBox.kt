package com.braintreepayments.api.uicomponents.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import com.braintreepayments.api.uicomponents.R

/**
 * A pulsing skeleton placeholder block for loading states — a solid box whose alpha animates back
 * and forth to signal that real content is on the way.
 *
 * Reusable across components: the caller sizes it via [modifier] (e.g. `Modifier.width(..).height(..)`)
 * and, optionally, matches the surrounding surface with [shape] and [color]. Multiple boxes placed in
 * a row/column form a skeleton of the eventual layout (see the FI loading chip in
 * [EditFiComponentView]).
 *
 * The pulse is tuned via [minAlpha] / [maxAlpha] / [durationMillis], which default to
 * [ShimmerBoxDefaults]; override them for a faster/slower or more/less pronounced shimmer.
 *
 * @param modifier       sizing / layout for the block. Give it an explicit width and height.
 * @param shape          the block's shape; defaults to a plain rectangle.
 * @param color          the base fill color (its alpha is animated); defaults to [R.color.shimmer].
 * @param minAlpha       the block's opacity at the dimmest point of the pulse.
 * @param maxAlpha       the block's opacity at the brightest point of the pulse.
 * @param durationMillis duration of one half of the pulse (dim → bright).
 */
@Composable
internal fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = colorResource(R.color.shimmer),
    minAlpha: Float = ShimmerBoxDefaults.MinAlpha,
    maxAlpha: Float = ShimmerBoxDefaults.MaxAlpha,
    durationMillis: Int = ShimmerBoxDefaults.DurationMillis,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    Spacer(modifier = modifier.background(color = color.copy(alpha = alpha), shape = shape))
}
