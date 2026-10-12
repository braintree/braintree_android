@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import android.graphics.Color
import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodStyleResolverUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `default style shows the logo, label and credit messaging`() {
        val resolver = resolve(PayPalSavedPaymentMethodViewStyle())

        assertTrue(resolver.showPayPalLogo)
        assertTrue(resolver.showPayPalLabel)
        assertTrue(resolver.showPayPalCreditMessaging)
    }

    @Test
    fun `default style uses the SDK default colors`() {
        val resolver = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(Color.parseColor("#FFFFFF"), resolver.backgroundColor)
        assertEquals(Color.parseColor("#222222"), resolver.textColor)
        assertEquals(Color.TRANSPARENT, resolver.borderColor)
    }

    @Test
    fun `default style uses the SDK default sizes and spacing`() {
        val resolver = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(0f, resolver.horizontalPaddingDp, DELTA)
        assertEquals(10f, resolver.verticalPaddingDp, DELTA)
        assertEquals(0f, resolver.cornerRadiusDp, DELTA)
        assertEquals(0f, resolver.borderWidthDp, DELTA)
        assertEquals(24f, resolver.logoWidthDp, DELTA)
        assertEquals(6f, resolver.labelMarginStartDp, DELTA)
        assertEquals(16f, resolver.editIconSizeDp, DELTA)
        assertEquals(12f, resolver.fundingInstrumentMarginStartDp, DELTA)
    }

    @Test
    fun `default style uses each element's SDK default font size`() {
        val resolver = resolve(PayPalSavedPaymentMethodViewStyle())

        assertEquals(20f, resolver.labelFontSizeSp, DELTA)
        assertEquals(14f, resolver.fundingInstrumentTextFontSizeSp, DELTA)
        assertEquals(16f, resolver.creditMessagingFontSizeSp, DELTA)
    }

    @Test
    fun `default style leaves the height, font and link color unset`() {
        val resolver = resolve(PayPalSavedPaymentMethodViewStyle())

        assertNull(resolver.heightDp)
        assertNull(resolver.fontResId)
        assertNull(resolver.creditMessagingLinkColor)
    }

    @Test
    fun `visibility toggles use the merchant values`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                showPayPalLogo = false,
                showPayPalLabel = false,
                showPayPalCreditMessaging = false
            )
        )

        assertFalse(resolver.showPayPalLogo)
        assertFalse(resolver.showPayPalLabel)
        assertFalse(resolver.showPayPalCreditMessaging)
    }

    @Test
    fun `component appearance uses the merchant values`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(
                    backgroundColor = Color.BLACK,
                    textColor = Color.WHITE,
                    fontResId = FONT_RES_ID
                )
            )
        )

        assertEquals(Color.BLACK, resolver.backgroundColor)
        assertEquals(Color.WHITE, resolver.textColor)
        assertEquals(FONT_RES_ID, resolver.fontResId)
    }

    @Test
    fun `container style uses the merchant values`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                container = ContainerStyle(
                    heightDp = 56f,
                    horizontalPaddingDp = 16f,
                    verticalPaddingDp = 12f,
                    cornerRadiusDp = 8f,
                    borderColor = Color.RED,
                    borderWidthDp = 1f,
                    logo = PayPalLogoStyle(widthDp = 32f),
                    label = PayPalLabelStyle(marginStartDp = 4f),
                    fundingInstrument = FundingInstrumentStyle(editIconSizeDp = 20f, marginStartDp = 10f),
                    creditMessaging = CreditMessagingStyle(linkColor = Color.BLUE)
                )
            )
        )

        assertEquals(56f, resolver.heightDp)
        assertEquals(16f, resolver.horizontalPaddingDp, DELTA)
        assertEquals(12f, resolver.verticalPaddingDp, DELTA)
        assertEquals(8f, resolver.cornerRadiusDp, DELTA)
        assertEquals(Color.RED, resolver.borderColor)
        assertEquals(1f, resolver.borderWidthDp, DELTA)
        assertEquals(32f, resolver.logoWidthDp, DELTA)
        assertEquals(4f, resolver.labelMarginStartDp, DELTA)
        assertEquals(20f, resolver.editIconSizeDp, DELTA)
        assertEquals(10f, resolver.fundingInstrumentMarginStartDp, DELTA)
        assertEquals(Color.BLUE, resolver.creditMessagingLinkColor)
    }

    @Test
    fun `element font sizes take precedence over the base font size`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(baseFontSizeSp = 18f),
                container = ContainerStyle(
                    label = PayPalLabelStyle(fontSizeSp = 22f),
                    fundingInstrument = FundingInstrumentStyle(textFontSizeSp = 13f),
                    creditMessaging = CreditMessagingStyle(fontSizeSp = 15f)
                )
            )
        )

        assertEquals(22f, resolver.labelFontSizeSp, DELTA)
        assertEquals(13f, resolver.fundingInstrumentTextFontSizeSp, DELTA)
        assertEquals(15f, resolver.creditMessagingFontSizeSp, DELTA)
    }

    @Test
    fun `base font size applies to elements without their own font size`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(componentAppearance = ComponentAppearance(baseFontSizeSp = 18f))
        )

        assertEquals(18f, resolver.labelFontSizeSp, DELTA)
        assertEquals(18f, resolver.fundingInstrumentTextFontSizeSp, DELTA)
        assertEquals(18f, resolver.creditMessagingFontSizeSp, DELTA)
    }

    @Test
    fun `negative sizes and spacing resolve to 0`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                container = ContainerStyle(
                    heightDp = -1f,
                    horizontalPaddingDp = -1f,
                    verticalPaddingDp = -1f,
                    cornerRadiusDp = -1f,
                    borderWidthDp = -1f,
                    logo = PayPalLogoStyle(widthDp = -1f),
                    label = PayPalLabelStyle(marginStartDp = -1f),
                    fundingInstrument = FundingInstrumentStyle(editIconSizeDp = -1f, marginStartDp = -1f)
                )
            )
        )

        assertEquals(0f, resolver.heightDp)
        assertEquals(0f, resolver.horizontalPaddingDp, DELTA)
        assertEquals(0f, resolver.verticalPaddingDp, DELTA)
        assertEquals(0f, resolver.cornerRadiusDp, DELTA)
        assertEquals(0f, resolver.borderWidthDp, DELTA)
        assertEquals(0f, resolver.logoWidthDp, DELTA)
        assertEquals(0f, resolver.labelMarginStartDp, DELTA)
        assertEquals(0f, resolver.editIconSizeDp, DELTA)
        assertEquals(0f, resolver.fundingInstrumentMarginStartDp, DELTA)
    }

    @Test
    fun `negative font sizes resolve to 0`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(baseFontSizeSp = -1f),
                container = ContainerStyle(label = PayPalLabelStyle(fontSizeSp = -1f))
            )
        )

        assertEquals(0f, resolver.labelFontSizeSp, DELTA)
        assertEquals(0f, resolver.fundingInstrumentTextFontSizeSp, DELTA)
        assertEquals(0f, resolver.creditMessagingFontSizeSp, DELTA)
    }

    @Test
    fun `zero values are kept instead of falling back to the default`() {
        val resolver = resolve(
            PayPalSavedPaymentMethodViewStyle(container = ContainerStyle(heightDp = 0f, verticalPaddingDp = 0f))
        )

        assertEquals(0f, resolver.heightDp)
        assertEquals(0f, resolver.verticalPaddingDp, DELTA)
    }

    private fun resolve(style: PayPalSavedPaymentMethodViewStyle) =
        PayPalSavedPaymentMethodStyleResolver(context, style)

    private companion object {
        const val DELTA = 0.001f
        const val FONT_RES_ID = 42
    }
}
