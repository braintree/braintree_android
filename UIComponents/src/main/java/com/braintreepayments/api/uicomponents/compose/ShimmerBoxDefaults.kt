package com.braintreepayments.api.uicomponents.compose

/**
 * Default pulse tuning for [ShimmerBox]. Override any of these by passing the corresponding
 * `minAlpha` / `maxAlpha` / `durationMillis` argument to [ShimmerBox].
 */
internal object ShimmerBoxDefaults {
    /** Opacity at the dimmest point of the pulse. */
    const val MinAlpha = 0.3f

    /** Opacity at the brightest point of the pulse. */
    const val MaxAlpha = 0.9f

    /** Duration of one half of the pulse (dim → bright), in milliseconds. */
    const val DurationMillis = 800
}
