package com.braintreepayments.api.uicomponents

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that PayPalButton and VenmoButton are never laid out smaller than their minimum size,
 * even when a parent tries to force a smaller size. The minimum width is derived from the logo plus
 * room for the focus-padding inset so the constant-size logo is never clipped; the minimum height is
 * the default button height. Mirrors the extreme sizes exercised in the Demo layout (a 20px-wide
 * Venmo button and a 5px-tall PayPal button in a LinearLayout).
 */
@RunWith(RobolectricTestRunner::class)
class ButtonLogoBoundsUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `venmo button forced narrow in a LinearLayout is never smaller than its minimum`() {
        val button = VenmoButton(context)
        val logo = ContextCompat.getDrawable(context, R.drawable.venmo_logo_white)!!
        val parent = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        parent.addView(button, LinearLayout.LayoutParams(TINY, LinearLayout.LayoutParams.WRAP_CONTENT))

        measureAndLayout(parent)

        assertTrue(button.width >= logo.intrinsicWidth + 4 * venmoFocusPadding())
        assertTrue(button.height >= defaultHeight())
    }

    @Test
    fun `paypal button forced short in a LinearLayout is never smaller than its minimum`() {
        val button = PayPalButton(context)
        val logo = ContextCompat.getDrawable(context, R.drawable.paypal_logo_black)!!
        val parent = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        parent.addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, TINY))

        measureAndLayout(parent)

        assertTrue(button.width >= logo.intrinsicWidth + 4 * paypalFocusPadding())
        assertTrue(button.height >= defaultHeight())
    }

    @Test
    fun `venmo button minimum width reserves room for its logo and inset`() {
        val button = VenmoButton(context)
        val logo = ContextCompat.getDrawable(context, R.drawable.venmo_logo_white)!!

        assertEquals(logo.intrinsicWidth + 4 * venmoFocusPadding(), button.minimumWidth)
    }

    @Test
    fun `paypal button minimum width reserves room for its logo and inset`() {
        val button = PayPalButton(context)
        val logo = ContextCompat.getDrawable(context, R.drawable.paypal_logo_black)!!

        assertEquals(logo.intrinsicWidth + 4 * paypalFocusPadding(), button.minimumWidth)
    }

    @Test
    fun `venmo button minimum height is the default button height`() {
        val button = VenmoButton(context)

        assertEquals(defaultHeight(), button.minimumHeight)
    }

    @Test
    fun `paypal button minimum height is the default button height`() {
        val button = PayPalButton(context)

        assertEquals(defaultHeight(), button.minimumHeight)
    }

    private fun defaultHeight() =
        context.resources.getDimension(R.dimen.pay_button_height).toInt()

    private fun paypalFocusPadding() =
        context.resources.getDimension(R.dimen.pay_button_focus_padding).toInt()

    private fun venmoFocusPadding() =
        2 * context.resources.getDimension(R.dimen.pay_button_focus_border).toInt()

    private fun measureAndLayout(view: View) {
        val spec = View.MeasureSpec.makeMeasureSpec(LARGE_BOUND, View.MeasureSpec.AT_MOST)
        view.measure(spec, spec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    companion object {
        private const val TINY = 20
        private const val LARGE_BOUND = 2000
    }
}