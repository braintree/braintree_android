package com.braintreepayments.api.uicomponents

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Appearance and loading-state assertions for [PaymentButtonRenderer], complementing
 * [PaymentButtonRendererUnitTest].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentButtonRendererAppearanceUnitTest {

    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var renderer: PaymentButtonRenderer
    private val style = PayPalButtonColor.Blue.toStyle()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        renderer = PaymentButtonRenderer(view, style)
        renderer.setup()
    }

    private fun fill(): Int =
        ((view.background as LayerDrawable).getDrawable(1) as GradientDrawable).color!!.defaultColor

    private fun colorOf(id: Int) = ContextCompat.getColor(context, id)

    @Test
    fun `default state uses the default fill`() {
        assertEquals(colorOf(style.default.fill), fill())
    }

    @Test
    fun `pressed state uses the pressed fill`() {
        view.isPressed = true

        renderer.updateAppearance()

        assertEquals(colorOf(style.pressed.fill), fill())
    }

    @Test
    fun `hovered state uses the hover fill`() {
        view.isHovered = true

        renderer.updateAppearance()

        assertEquals(colorOf(style.hover.fill), fill())
    }

    @Test
    fun `focused state uses the focus fill`() {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()
        assertTrue(view.isFocused)

        renderer.updateAppearance()

        assertEquals(colorOf(style.focus.fill), fill())
    }

    @Test
    fun `pressed takes precedence over hovered`() {
        view.isPressed = true
        view.isHovered = true

        renderer.updateAppearance()

        assertEquals(colorOf(style.pressed.fill), fill())
    }

    @Test
    fun `returning to the idle state restores the default fill`() {
        view.isPressed = true
        renderer.updateAppearance()
        view.isPressed = false

        renderer.updateAppearance()

        assertEquals(colorOf(style.default.fill), fill())
    }

    @Test
    fun `setStyle with a different style recolors the background`() {
        val before = fill()
        val black = PayPalButtonColor.Black.toStyle()

        renderer.setStyle(black)

        assertNotEquals(before, fill())
        assertEquals(colorOf(black.default.fill), fill())
    }

    @Test
    fun `hideLoading without a prior showLoading re-enables the view`() {
        view.isEnabled = false

        renderer.hideLoading()

        assertTrue(view.isEnabled)
    }

    @Test
    fun `showLoading then hideLoading can be repeated`() {
        repeat(2) {
            renderer.showLoading()
            assertEquals(false, view.isEnabled)
            renderer.hideLoading()
            assertTrue(view.isEnabled)
        }
    }
}
