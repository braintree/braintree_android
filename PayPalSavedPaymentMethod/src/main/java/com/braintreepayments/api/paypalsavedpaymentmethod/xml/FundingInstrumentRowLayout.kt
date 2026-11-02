package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * The row with the PayPal logo and label (first child), the loading shimmer (second) and the funding instrument chip
 * (third). The chip sits next to the logo and label, its start margin apart, when it fits at its full width; otherwise
 * it goes below them. The shimmer always sits next to the logo and label and fills the rest of the row.
 */
internal class FundingInstrumentRowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val stackedSpacing =
        resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_stacked_layout_spacing)

    /** Whether the chip is below the logo and label. */
    internal var isStacked = false
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val brand = getChildAt(BRAND)
        val shimmer = getChildAt(SHIMMER)
        val chip = getChildAt(CHIP)
        val isBounded = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)

        brand.measure(UNSPECIFIED, UNSPECIFIED)
        val brandWidth = if (brand.isGone) 0 else brand.measuredWidth
        val brandHeight = if (brand.isGone) 0 else brand.measuredHeight
        isStacked = false
        var width = brandWidth
        var height = brandHeight

        val trailing = if (shimmer.isVisible) shimmer else chip.takeIf { it.isVisible }
        if (trailing != null) {
            val gap = gapBefore(trailing)
            val available = (maxWidth - brandWidth - gap).coerceAtLeast(0)
            if (trailing === shimmer) {
                shimmer.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY), UNSPECIFIED)
            } else {
                chip.measure(UNSPECIFIED, UNSPECIFIED)
                isStacked = isBounded && !brand.isGone && chip.measuredWidth > available
                val chipMaxWidth = if (isStacked) maxWidth else available
                if (isBounded) chip.measure(MeasureSpec.makeMeasureSpec(chipMaxWidth, MeasureSpec.AT_MOST), UNSPECIFIED)
            }
            if (isStacked) {
                width = maxOf(brandWidth, trailing.measuredWidth)
                height = brandHeight + stackedSpacing + trailing.measuredHeight
            } else {
                width = brandWidth + gap + trailing.measuredWidth
                height = maxOf(brandHeight, trailing.measuredHeight)
            }
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val brand = getChildAt(BRAND)
        val trailing = getChildAt(SHIMMER).takeIf { it.isVisible } ?: getChildAt(CHIP).takeIf { it.isVisible }
        val rowHeight = bottom - top
        val brandWidth = if (brand.isGone) 0 else brand.measuredWidth
        if (!brand.isGone) {
            val brandTop = if (isStacked) 0 else (rowHeight - brand.measuredHeight) / 2
            placeChild(brand, start = 0, top = brandTop)
        }
        if (trailing == null) return
        if (isStacked) {
            placeChild(trailing, start = 0, top = brand.measuredHeight + stackedSpacing)
        } else {
            placeChild(trailing, brandWidth + gapBefore(trailing), (rowHeight - trailing.measuredHeight) / 2)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams = MarginLayoutParams(context, attrs)

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(params: LayoutParams): LayoutParams = MarginLayoutParams(params)

    override fun checkLayoutParams(params: LayoutParams?): Boolean = params is MarginLayoutParams

    // The start margin separates the shimmer or chip from the logo and label.
    private fun gapBefore(child: View): Int = if (getChildAt(BRAND).isGone) 0 else child.marginStart

    // Positions from the start edge, mirrored for right-to-left layouts.
    private fun placeChild(child: View, start: Int, top: Int) {
        val childLeft = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            width - start - child.measuredWidth
        } else {
            start
        }
        child.layout(childLeft, top, childLeft + child.measuredWidth, top + child.measuredHeight)
    }

    private companion object {
        const val BRAND = 0
        const val SHIMMER = 1
        const val CHIP = 2
        val UNSPECIFIED = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    }
}
