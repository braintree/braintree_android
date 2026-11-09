package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.View.MeasureSpec
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ShimmerPlaceholderViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `measures to the placeholder height and the width it's given`() {
        val sut = ShimmerPlaceholderView(context)

        sut.measure(
            MeasureSpec.makeMeasureSpec(300, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        assertEquals(300, sut.measuredWidth)
        assertEquals(
            context.resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_placeholder_height),
            sut.measuredHeight
        )
    }

    @Test
    fun `sweeps only while attached, shown and the window is visible`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val sut = ShimmerPlaceholderView(activity)
        assertFalse(sut.isAnimating)

        activity.setContentView(sut)
        idle()
        // Robolectric reports the activity window as gone; a device reports it visible once the view is attached.
        sut.dispatchWindowVisibilityChanged(View.VISIBLE)
        assertTrue(sut.isAnimating)

        sut.visibility = View.GONE
        assertFalse(sut.isAnimating)

        sut.visibility = View.VISIBLE
        assertTrue(sut.isAnimating)

        sut.dispatchWindowVisibilityChanged(View.GONE)
        assertFalse(sut.isAnimating)

        sut.dispatchWindowVisibilityChanged(View.VISIBLE)
        activity.setContentView(View(activity))
        idle()
        assertFalse(sut.isAnimating)
    }

    @Test
    fun `stays static when animations are turned off`() {
        val sut = shownShimmer()
        assertTrue(sut.isAnimating)

        setAnimatorDurationScale(0f)
        try {
            // Shown again, as when the buyer comes back after changing the setting.
            sut.visibility = View.GONE
            sut.visibility = View.VISIBLE

            assertFalse(sut.isAnimating)
        } finally {
            setAnimatorDurationScale(1f)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun `stays static when animations are turned off before Android 8`() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

        assertFalse(shownShimmer().isAnimating)
    }

    @Test
    fun `leaves the trailing gap on the right in a left-to-right layout`() {
        val sut = layOut(View.LAYOUT_DIRECTION_LTR)

        assertEquals(0f, sut.barBounds.left)
        assertEquals((WIDTH - trailingGap()).toFloat(), sut.barBounds.right)
    }

    @Test
    fun `leaves the trailing gap on the left in a right-to-left layout`() {
        val sut = layOut(View.LAYOUT_DIRECTION_RTL)

        assertEquals(trailingGap().toFloat(), sut.barBounds.left)
        assertEquals(WIDTH.toFloat(), sut.barBounds.right)
    }

    @Test
    fun `an exact height from the layout replaces the bar height`() {
        val sut = ShimmerPlaceholderView(context)

        sut.measure(
            MeasureSpec.makeMeasureSpec(28, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(18, MeasureSpec.EXACTLY)
        )

        assertEquals(28, sut.measuredWidth)
        assertEquals(18, sut.measuredHeight)
    }

    @Test
    fun `is hidden from screen readers unless it has a content description`() {
        val sut = ShimmerPlaceholderView(context)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, sut.importantForAccessibility)

        sut.contentDescription = "Loading saved payment method"
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_YES, sut.importantForAccessibility)

        sut.contentDescription = null
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, sut.importantForAccessibility)
    }

    // Attached, shown and in a visible window, so it would otherwise sweep.
    private fun shownShimmer(): ShimmerPlaceholderView {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val sut = ShimmerPlaceholderView(activity)
        activity.setContentView(sut)
        idle()
        sut.dispatchWindowVisibilityChanged(View.VISIBLE)
        return sut
    }

    // The system "Animator duration scale" setting, which ValueAnimator.areAnimatorsEnabled() reads from Android 8.
    private fun setAnimatorDurationScale(scale: Float) {
        ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, scale)
    }

    private fun layOut(direction: Int) = ShimmerPlaceholderView(context).apply {
        layoutDirection = direction
        measure(
            MeasureSpec.makeMeasureSpec(WIDTH, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private fun trailingGap() =
        context.resources.getDimensionPixelSize(R.dimen.paypal_saved_payment_method_placeholder_trailing_gap)

    private companion object {
        const val WIDTH = 300
    }
}
