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
import org.robolectric.annotation.Config

// xhdpi (density 2), so a dp value that skipped its conversion would be off by 2x.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "xhdpi")
class StyleAttrsParserUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `styleFromAttrs leaves every field unset when no attrs are set`() {
        val style = styleFromAttrs(context, Robolectric.buildAttributeSet().build(), 0)

        assertTrue(style.showPayPalLogo)
        assertTrue(style.showPayPalLabel)
        assertTrue(style.showPayPalCreditMessaging)
        val appearance = requireNotNull(style.componentAppearance)
        assertNull(appearance.backgroundColor)
        assertNull(appearance.textColor)
        assertNull(appearance.baseFontSizeSp)
        assertNull(appearance.fontResId)
        val container = requireNotNull(style.container)
        assertNull(container.heightDp)
        assertNull(container.horizontalPaddingDp)
        assertNull(container.verticalPaddingDp)
        assertNull(container.cornerRadiusDp)
        assertNull(container.borderColor)
        assertNull(container.borderWidthDp)
        assertNull(container.logo?.widthDp)
        assertNull(container.label?.fontSizeSp)
        assertNull(container.label?.marginStartDp)
        assertNull(container.fundingInstrument?.textFontSizeSp)
        assertNull(container.fundingInstrument?.editIconSizeDp)
        assertNull(container.fundingInstrument?.marginStartDp)
        assertNull(container.creditMessaging?.fontSizeSp)
        assertNull(container.creditMessaging?.linkColor)
    }

    @Test
    fun `styleFromAttrs parses the visibility attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypalSavedPaymentMethodShowLogo, "false")
            .addAttribute(R.attr.paypalSavedPaymentMethodShowLabel, "false")
            .addAttribute(R.attr.paypalSavedPaymentMethodShowCreditMessaging, "false")
            .build()

        val style = styleFromAttrs(context, attrs, 0)

        assertFalse(style.showPayPalLogo)
        assertFalse(style.showPayPalLabel)
        assertFalse(style.showPayPalCreditMessaging)
    }

    @Test
    fun `styleFromAttrs parses the component appearance attrs`() {
        RuntimeEnvironment.setFontScale(LARGE_FONT_SCALE)
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypalSavedPaymentMethodBackgroundColor, "#0000FF")
            .addAttribute(R.attr.paypalSavedPaymentMethodTextColor, "#FF0000")
            .addAttribute(R.attr.paypalSavedPaymentMethodFontSize, "18sp")
            .build()

        val appearance = styleFromAttrs(context, attrs, 0).componentAppearance

        assertEquals(Color.BLUE, appearance?.backgroundColor)
        assertEquals(Color.RED, appearance?.textColor)
        assertEquals(18f, appearance?.baseFontSizeSp)
    }

    @Test
    fun `styleFromAttrs parses the font attr as a resource ID`() {
        // The module ships no font, and the parser reads any resource reference, so a drawable stands in for one.
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypalSavedPaymentMethodFont, "@drawable/ic_edit_pencil")
            .build()

        val appearance = styleFromAttrs(context, attrs, 0).componentAppearance

        assertEquals(R.drawable.ic_edit_pencil, appearance?.fontResId)
    }

    @Test
    fun `styleFromAttrs parses the container attrs`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypalSavedPaymentMethodHeight, "48dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodHorizontalPadding, "16dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodVerticalPadding, "12dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodCornerRadius, "8dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodBorderColor, "#00FF00")
            .addAttribute(R.attr.paypalSavedPaymentMethodBorderWidth, "1dp")
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
        RuntimeEnvironment.setFontScale(LARGE_FONT_SCALE)
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paypalSavedPaymentMethodLogoWidth, "32dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodLabelFontSize, "18sp")
            .addAttribute(R.attr.paypalSavedPaymentMethodLabelMarginStart, "4dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodFundingInstrumentFontSize, "13sp")
            .addAttribute(R.attr.paypalSavedPaymentMethodEditIconSize, "20dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodFundingInstrumentMarginStart, "10dp")
            .addAttribute(R.attr.paypalSavedPaymentMethodCreditMessagingFontSize, "15sp")
            .addAttribute(R.attr.paypalSavedPaymentMethodCreditMessagingLinkColor, "#FF00FF")
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

    private companion object {
        const val LARGE_FONT_SCALE = 2f
    }
}
