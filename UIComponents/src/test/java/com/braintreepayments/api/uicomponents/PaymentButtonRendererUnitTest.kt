package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.View.MeasureSpec
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
class PaymentButtonRendererUnitTest {

    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var renderer: PaymentButtonRenderer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        renderer = PaymentButtonRenderer(view, PayPalButtonColor.Blue.toStyle())
        renderer.setup()
    }

    @Test
    fun `setup installs a layered background on the view`() {
        assertTrue(view.background is LayerDrawable)
        assertEquals(2, (view.background as LayerDrawable).numberOfLayers)
    }

    @Test
    fun `setup sets minimum height and a width that fits the logo with padding`() {
        val logo = ContextCompat.getDrawable(context, PayPalButtonColor.Blue.logoId)!!
        val focusPadding = dimen(R.dimen.pay_button_focus_padding)

        assertEquals(dimen(R.dimen.pay_button_height), view.minimumHeight)
        assertEquals(logo.intrinsicWidth + 4 * focusPadding, view.minimumWidth)
    }

    @Test
    fun `setStyle updates the current style`() {
        val black = PayPalButtonColor.Black.toStyle()

        renderer.setStyle(black)

        assertEquals(black, renderer.style)
    }

    @Test
    fun `setStyle with the same style keeps the existing instance`() {
        val before = renderer.style

        renderer.setStyle(PayPalButtonColor.Blue.toStyle())

        assertSame(before, renderer.style)
    }

    @Test
    fun `onMeasure uses desired size when unspecified`() {
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        val (width, height) = renderer.onMeasure(spec, spec)

        assertEquals(maxOf(dimen(R.dimen.pay_button_width), view.minimumWidth), width)
        assertEquals(dimen(R.dimen.pay_button_height), height)
    }

    @Test
    fun `onMeasure clamps to the minimum size`() {
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)

        val (width, height) = renderer.onMeasure(spec, spec)

        assertEquals(view.minimumWidth, width)
        assertEquals(view.minimumHeight, height)
    }

    @Test
    fun `onDraw before and after loading does not throw`() {
        val canvas = Canvas()

        renderer.onDraw(canvas)
        renderer.showLoading()
        renderer.onDraw(canvas)
        renderer.hideLoading()
        renderer.onDraw(canvas)
    }

    @Test
    fun `updateAppearance applies style for the pressed state`() {
        view.isPressed = true

        renderer.updateAppearance()

        assertNotNull(view.background)
    }

    @Test
    fun `showLoading disables the view and hideLoading re-enables it`() {
        assertTrue(view.isEnabled)

        renderer.showLoading()
        assertFalse(view.isEnabled)

        renderer.hideLoading()
        assertTrue(view.isEnabled)
    }

    private fun dimen(id: Int) = context.resources.getDimension(id).toInt()
}
