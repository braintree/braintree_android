package com.braintreepayments.api.uicomponents

import android.content.Context
import android.view.View.MeasureSpec
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PayPalButtonTest {

    @Test
    fun payPalButton_canBeInstantiated() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        assertNotNull(button)
    }

    @Test
    fun payPalButton_isEnabledByDefault() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        assertTrue(button.isEnabled)
    }

    @Test
    fun setButtonColor_toBlack_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.Black)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toWhite_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.White)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toSameColor_doesNotThrow() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(PayPalButtonColor.Blue)
        button.setButtonColor(PayPalButtonColor.Blue)

        assertNotNull(button)
    }

    @Test
    fun setEnabled_false_disablesButton() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())

        button.isEnabled = false

        assertFalse(button.isEnabled)
    }

    @Test
    fun setEnabled_trueAfterFalse_reenablesButton() {
        val button = PayPalButton(ApplicationProvider.getApplicationContext())
        button.isEnabled = false

        button.isEnabled = true

        assertTrue(button.isEnabled)
    }

    @Test
    fun onMeasure_withUnspecifiedSpec_usesDesiredSizeClampedToMinimum() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayPalButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(maxOf(desiredWidth(context), minWidth(context)), button.measuredWidth)
        assertEquals(maxOf(desiredHeight(context), minHeight(context)), button.measuredHeight)
    }

    @Test
    fun onMeasure_withWrapContentSpec_usesDesiredSizeClampedToMinimum() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayPalButton(context)
        val spec = MeasureSpec.makeMeasureSpec(LARGE_BUTTON_SIZE, MeasureSpec.AT_MOST)

        button.measure(spec, spec)

        assertEquals(maxOf(desiredWidth(context), minWidth(context)), button.measuredWidth)
        assertEquals(maxOf(desiredHeight(context), minHeight(context)), button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactSpec_usesProvidedSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayPalButton(context)
        val exactWidth = desiredWidth(context) + LARGE_BUTTON_SIZE
        val exactHeight = desiredHeight(context) + LARGE_BUTTON_SIZE
        val widthSpec = MeasureSpec.makeMeasureSpec(exactWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(exactHeight, MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(exactWidth, button.measuredWidth)
        assertEquals(exactHeight, button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactWidthBelowMinimum_clampsToFitLogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayPalButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight(context), MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minWidth(context), button.measuredWidth)
        assertTrue(button.measuredWidth >= logo(context).intrinsicWidth)
    }

    @Test
    fun onMeasure_withExactHeightBelowMinimum_clampsToFitLogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = PayPalButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(desiredWidth(context), MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minHeight(context), button.measuredHeight)
        assertTrue(button.measuredHeight >= logo(context).intrinsicHeight)
    }

    private fun desiredWidth(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_width).toInt()

    private fun desiredHeight(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_height).toInt()

    private fun focusPadding(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_focus_padding).toInt()

    private fun logo(context: Context) =
        ContextCompat.getDrawable(context, R.drawable.paypal_logo_black)!!

    private fun minWidth(context: Context) =
        logo(context).intrinsicWidth + 4 * focusPadding(context)

    private fun minHeight(context: Context) =
        desiredHeight(context)

    companion object {
        private const val LARGE_BUTTON_SIZE = 2000
    }
}
