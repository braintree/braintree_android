package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * A rounded placeholder bar with a white highlight sweeping left to right, shown while the funding instrument or the
 * Pay Later message is loading. The bar fills the width it's given and stops short of the trailing edge.
 *
 * Set [setContentDescription] for what screen readers announce, or leave it `null` to hide the bar from screen readers,
 * for a bar shown next to one that already announces loading.
 */
internal class ShimmerPlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barHeight = resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_placeholder_height)
    private val trailingGap =
        resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_placeholder_trailing_gap)
    private val cornerRadius = resources.getDimension(R.dimen.paypal_saved_payment_method_placeholder_corner_radius)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.paypal_saved_payment_method_placeholder)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightMatrix = Matrix()
    private val barRect = RectF()
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SWEEP_DURATION_MILLIS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    // False while the View constructor runs, which can report a visibility change before the animator exists.
    private var isInitialized = false
    private var isWindowVisible = true

    /** The bar's bounds within the view. */
    @get:VisibleForTesting
    internal val barBounds: RectF
        get() = RectF(barRect)

    /** Whether the highlight is sweeping. */
    internal val isAnimating: Boolean
        get() = animator.isStarted

    init {
        if (contentDescription == null) importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isInitialized = true
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        importantForAccessibility =
            if (contentDescription == null) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), barHeight)
    }

    // Recomputed on every layout, not only on a size change, because changing the layout direction lays the view out
    // again without resizing it. The trailing gap is on the end side, like padding(end) in the Compose
    // ShimmerPlaceholder: the right in left-to-right layouts and the left in right-to-left ones.
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val barWidth = (width - trailingGap).coerceAtLeast(0).toFloat()
        val barLeft = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - barWidth else 0f
        barRect.set(barLeft, 0f, barLeft + barWidth, height.toFloat())
        highlightPaint.shader = LinearGradient(
            0f,
            0f,
            barWidth * HIGHLIGHT_WIDTH_FRACTION,
            0f,
            intArrayOf(
                Color.TRANSPARENT,
                ColorUtils.setAlphaComponent(Color.WHITE, HIGHLIGHT_ALPHA),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        // The highlight starts fully off the left edge and ends fully off the right edge.
        val bandWidth = barRect.width() * HIGHLIGHT_WIDTH_FRACTION
        highlightMatrix.setTranslate(barRect.left - bandWidth + progress * (barRect.width() + bandWidth), 0f)
        highlightPaint.shader?.setLocalMatrix(highlightMatrix)
        canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, highlightPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isWindowVisible = visibility == VISIBLE
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    // Animates only while on screen, and not while the app is in the background.
    private fun updateAnimation() {
        if (!isInitialized) return
        if (isAttachedToWindow && isShown && isWindowVisible) {
            if (!animator.isStarted) animator.start()
        } else {
            animator.cancel()
        }
    }

    // The same sweep as the Compose ShimmerPlaceholder, so both views load the same way.
    private companion object {
        const val SWEEP_DURATION_MILLIS = 1200L
        const val HIGHLIGHT_WIDTH_FRACTION = 0.6f
        const val HIGHLIGHT_ALPHA = (0.65f * 255).toInt()
    }
}
