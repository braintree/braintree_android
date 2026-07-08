package com.braintreepayments.api.uicomponents

import android.content.Context
import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoButtonTest {

    @Test
    fun venmoButton_canBeInstantiated() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        assertNotNull(button)
    }

    @Test
    fun venmoButton_isEnabledByDefault() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        assertTrue(button.isEnabled)
    }

    @Test
    fun setButtonColor_toBlack_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.Black)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toWhite_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.White)

        assertNotNull(button)
    }

    @Test
    fun setButtonColor_toSameColor_doesNotThrow() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.setButtonColor(VenmoButtonColor.Blue)
        button.setButtonColor(VenmoButtonColor.Blue)

        assertNotNull(button)
    }

    @Test
    fun setEnabled_false_disablesButton() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())

        button.isEnabled = false

        assertFalse(button.isEnabled)
    }

    @Test
    fun setEnabled_trueAfterFalse_reenablesButton() {
        val button = VenmoButton(ApplicationProvider.getApplicationContext())
        button.isEnabled = false

        button.isEnabled = true

        assertTrue(button.isEnabled)
    }

    @Test
    fun onMeasure_withUnspecifiedSpec_usesDesiredSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = VenmoButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(desiredWidth(context), button.measuredWidth)
        assertEquals(desiredHeight(context), button.measuredHeight)
    }

    @Test
    fun onMeasure_withWrapContentSpec_usesDesiredSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = VenmoButton(context)
        val spec = MeasureSpec.makeMeasureSpec(LARGE_BOUND, MeasureSpec.AT_MOST)

        button.measure(spec, spec)

        assertEquals(desiredWidth(context), button.measuredWidth)
        assertEquals(desiredHeight(context), button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactSpec_usesProvidedSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = VenmoButton(context)
        val exactWidth = desiredWidth(context) + LARGE_BOUND
        val exactHeight = desiredHeight(context) + LARGE_BOUND
        val widthSpec = MeasureSpec.makeMeasureSpec(exactWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(exactHeight, MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(exactWidth, button.measuredWidth)
        assertEquals(exactHeight, button.measuredHeight)
    }

    @Test
    fun onMeasure_withExactWidthBelowMinimum_clampsToMinimumWidth() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val button = VenmoButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight(context), MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minDesiredWidth(context), button.measuredWidth)
    }

    private fun desiredWidth(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_width).toInt()

    private fun desiredHeight(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_height).toInt()

    private fun minDesiredWidth(context: Context) =
        context.resources.getDimension(R.dimen.pay_button_min_width).toInt()

    companion object {
        private const val LARGE_BOUND = 2000
    }
}
