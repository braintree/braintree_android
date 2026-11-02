package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * A loading placeholder bar with a sweeping highlight. A `null` [contentDescription] hides it from screen readers.
 */
@Composable
internal fun ShimmerPlaceholder(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    height: Dp = dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_height),
    trailingGap: Dp = dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_trailing_gap)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-sweep"
    )
    val shape = RoundedCornerShape(dimensionResource(R.dimen.paypal_saved_payment_method_placeholder_corner_radius))

    Spacer(
        // Our semantics come before the caller's modifier, so they replace any description the caller set.
        modifier = Modifier
            .clearAndSetSemantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .then(modifier)
            // A Spacer has no width of its own; this fills whatever width the caller's modifier allows.
            .fillMaxWidth()
            .padding(end = trailingGap)
            .height(height)
            .clip(shape)
            .background(colorResource(R.color.paypal_saved_payment_method_placeholder))
            .drawWithContent {
                drawContent()
                val bandWidth = size.width * HIGHLIGHT_WIDTH_FRACTION
                val start = -bandWidth + progress * (size.width + bandWidth)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = HIGHLIGHT_ALPHA),
                            Color.Transparent
                        ),
                        startX = start,
                        endX = start + bandWidth
                    )
                )
            }
    )
}

private const val SWEEP_DURATION_MILLIS = 1200
private const val HIGHLIGHT_WIDTH_FRACTION = 0.6f
private const val HIGHLIGHT_ALPHA = 0.65f
