@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class XmlStyleExtensionsUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val resolvedStyle = PayPalSavedPaymentMethodStyleResolver(
        context,
        PayPalSavedPaymentMethodViewStyle(componentAppearance = ComponentAppearance(textColor = Color.RED))
    )

    @Test
    @Config(qualifiers = "xhdpi")
    fun `dpToPx converts dp to whole pixels for the screen density`() {
        assertEquals(32, 16f.dpToPx(context.resources))
        assertEquals(0, 0f.dpToPx(context.resources))
    }

    @Test
    @Config(qualifiers = "ldpi")
    fun `dpToPx rounds, so a thin border isn't lost on a low density screen`() {
        assertEquals(1, 1f.dpToPx(context.resources))
    }

    @Test
    fun `applyTextStyle sets the text color, the size in sp and the system font`() {
        val textView = TextView(context)

        textView.applyTextStyle(resolvedStyle, sizeSp = 14f)

        assertEquals(Color.RED, textView.currentTextColor)
        assertEquals(14f, textView.textSize / context.resources.displayMetrics.scaledDensity, DELTA)
        assertEquals(Typeface.DEFAULT, textView.typeface)
    }

    @Test
    fun `applyTextStyle uses the medium system font for medium text`() {
        val textView = TextView(context)

        textView.applyTextStyle(resolvedStyle, sizeSp = 20f, medium = true)

        assertNotEquals(Typeface.DEFAULT, textView.typeface)
    }

    @After
    fun tearDown() {
        unmockkStatic(ResourcesCompat::class)
    }

    @Test
    fun `applyTextStyle uses the merchant's font as is for regular text`() {
        val font = givenMerchantFont()
        val textView = TextView(context)

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 14f)

        assertSame(font, textView.typeface)
    }

    @Test
    @Config(sdk = [28])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `applyTextStyle uses the merchant's font at medium weight on Android 9 and later`() {
        val font = givenMerchantFont()
        val textView = TextView(context)

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 20f, medium = true)

        assertNotEquals(font, textView.typeface)
        assertEquals(500, textView.typeface.weight)
    }

    @Test
    @Config(sdk = [27])
    fun `applyTextStyle keeps the merchant's font for medium text before Android 9`() {
        val font = givenMerchantFont()
        val textView = TextView(context)

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 20f, medium = true)

        assertSame(font, textView.typeface)
    }

    private fun givenMerchantFont(): Typeface {
        val font = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        mockkStatic(ResourcesCompat::class)
        every { ResourcesCompat.getFont(any(), FONT_RES) } returns font
        return font
    }

    private fun merchantFontStyle() = PayPalSavedPaymentMethodStyleResolver(
        context,
        PayPalSavedPaymentMethodViewStyle(componentAppearance = ComponentAppearance(fontResId = FONT_RES))
    )

    private companion object {
        const val FONT_RES = 0x7f0b0001
        const val DELTA = 0.01f
    }
}
