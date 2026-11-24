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
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/** A loading bar with a sweeping highlight, static when animations are turned off. */
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
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            1f,
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
    private val highlightMatrix = Matrix()
    private val barRect = RectF()
    private var progress = 0f

    private var animator: ValueAnimator? = null
    private var isWindowVisible = true

    /** Whether the bar stops short of the trailing edge. */
    var showsTrailingGap = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    @get:VisibleForTesting
    val barBounds: RectF
        get() = RectF(barRect)

    @get:VisibleForTesting
    val isAnimating: Boolean
        get() = animator?.isStarted == true

    init {
        if (contentDescription == null) importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        importantForAccessibility =
            if (contentDescription == null) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(barHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val barWidth = (width - if (showsTrailingGap) trailingGap else 0).coerceAtLeast(0).toFloat()
        val barLeft = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - barWidth else 0f
        barRect.set(barLeft, 0f, barLeft + barWidth, height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
        val bandWidth = barRect.width() * HIGHLIGHT_WIDTH_FRACTION
        highlightMatrix.setScale(bandWidth, 1f)
        highlightMatrix.postTranslate(barRect.left - bandWidth + progress * (barRect.width() + bandWidth), 0f)
        highlightPaint.shader.setLocalMatrix(highlightMatrix)
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
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun updateAnimation() {
        val isOnScreen = isAttachedToWindow && isShown && isWindowVisible
        if (isOnScreen && areAnimationsEnabled()) {
            val sweep = animator ?: ValueAnimator.ofFloat(0f, 1f).apply {
                duration = SWEEP_DURATION_MILLIS
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
            }.also { animator = it }
            if (!sweep.isStarted) sweep.start()
        } else {
            animator?.cancel()
            if (progress != 0f) {
                progress = 0f
                invalidate()
            }
        }
    }

    private fun areAnimationsEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }

    private companion object {
        const val SWEEP_DURATION_MILLIS = 1200L
        const val HIGHLIGHT_WIDTH_FRACTION = 0.6f
        const val HIGHLIGHT_ALPHA = (0.65f * 255).toInt()
    }
}
