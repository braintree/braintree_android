@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import android.graphics.Color
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StyleAttrsParserUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `styleFromAttrs leaves every field unset when no attrs are set`() {
        val style = styleFromAttrs(context, Robolectric.buildAttributeSet().build(), 0)

        assertTrue(style.showPayPalLogo)
        assertTrue(style.showPayPalLabel)
        assertTrue(style.showPayPalCreditMessaging)
        assertNull(style.componentAppearance?.backgroundColor)
        assertNull(style.componentAppearance?.baseFontSizeSp)
        assertNull(style.container?.heightDp)
        assertNull(style.container?.horizontalPaddingDp)
        assertNull(style.container?.logo?.widthDp)
    }

    @Test
    fun `styleFromAttrs parses the visibility attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypal_saved_payment_method_show_logo, "false")
            .addAttribute(R.attr.paypal_saved_payment_method_show_label, "false")
            .addAttribute(R.attr.paypal_saved_payment_method_show_credit_messaging, "false")
            .build()

        val style = styleFromAttrs(context, attrs, 0)

        assertFalse(style.showPayPalLogo)
        assertFalse(style.showPayPalLabel)
        assertFalse(style.showPayPalCreditMessaging)
    }

    @Test
    fun `styleFromAttrs parses the component appearance attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypal_saved_payment_method_background_color, "#0000FF")
            .addAttribute(R.attr.paypal_saved_payment_method_text_color, "#FF0000")
            .addAttribute(R.attr.paypal_saved_payment_method_font_size, "18sp")
            .build()

        val appearance = styleFromAttrs(context, attrs, 0).componentAppearance

        assertEquals(Color.BLUE, appearance?.backgroundColor)
        assertEquals(Color.RED, appearance?.textColor)
        assertEquals(18f, appearance?.baseFontSizeSp)
    }

    @Test
    fun `styleFromAttrs parses the container attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypal_saved_payment_method_height, "48dp")
            .addAttribute(R.attr.paypal_saved_payment_method_horizontal_padding, "16dp")
            .addAttribute(R.attr.paypal_saved_payment_method_vertical_padding, "12dp")
            .addAttribute(R.attr.paypal_saved_payment_method_corner_radius, "8dp")
            .addAttribute(R.attr.paypal_saved_payment_method_border_color, "#00FF00")
            .addAttribute(R.attr.paypal_saved_payment_method_border_width, "1dp")
            .build()

        val container = styleFromAttrs(context, attrs, 0).container

        assertEquals(48f, container?.heightDp)
        assertEquals(16f, container?.horizontalPaddingDp)
        assertEquals(12f, container?.verticalPaddingDp)
        assertEquals(8f, container?.cornerRadiusDp)
        assertEquals(Color.GREEN, container?.borderColor)
        assertEquals(1f, container?.borderWidthDp)
    }

    @Test
    fun `styleFromAttrs parses the logo, label, funding instrument and credit messaging attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypal_saved_payment_method_logo_width, "32dp")
            .addAttribute(R.attr.paypal_saved_payment_method_label_font_size, "18sp")
            .addAttribute(R.attr.paypal_saved_payment_method_label_margin_start, "4dp")
            .addAttribute(R.attr.paypal_saved_payment_method_funding_instrument_font_size, "13sp")
            .addAttribute(R.attr.paypal_saved_payment_method_edit_icon_size, "20dp")
            .addAttribute(R.attr.paypal_saved_payment_method_funding_instrument_margin_start, "10dp")
            .addAttribute(R.attr.paypal_saved_payment_method_credit_messaging_font_size, "15sp")
            .addAttribute(R.attr.paypal_saved_payment_method_credit_messaging_link_color, "#FF00FF")
            .build()

        val container = styleFromAttrs(context, attrs, 0).container

        assertEquals(32f, container?.logo?.widthDp)
        assertEquals(18f, container?.label?.fontSizeSp)
        assertEquals(4f, container?.label?.marginStartDp)
        assertEquals(13f, container?.fundingInstrument?.textFontSizeSp)
        assertEquals(20f, container?.fundingInstrument?.editIconSizeDp)
        assertEquals(10f, container?.fundingInstrument?.marginStartDp)
        assertEquals(15f, container?.creditMessaging?.fontSizeSp)
        assertEquals(Color.MAGENTA, container?.creditMessaging?.linkColor)
    }
}
