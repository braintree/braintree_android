package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Activity
import android.content.Context
import android.os.Looper
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
import org.robolectric.Shadows.shadowOf

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
        shadowOf(Looper.getMainLooper()).idle()
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
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(sut.isAnimating)
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
}
