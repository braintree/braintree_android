@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodAppearance
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
    private val resolvedStyle = styleWith(PayPalSavedPaymentMethodAppearance(textColor = Color.RED))
    private val textView = TextView(context)

    @After
    fun tearDown() {
        unmockkStatic(ResourcesCompat::class)
    }

    @Test
    @Config(qualifiers = "xhdpi")
    fun `dpToPx converts dp to whole pixels for the screen density`() {
        assertEquals(32, 16f.dpToPx(context.resources))
        assertEquals(0, 0f.dpToPx(context.resources))
    }

    @Test
    @Config(qualifiers = "ldpi")
    fun `dpToPx keeps a thin border at least one pixel on a low density screen`() {
        assertEquals(1, 1f.dpToPx(context.resources))
        // 0.5dp is 0.375px on ldpi, which rounds to 0.
        assertEquals(1, 0.5f.dpToPx(context.resources))
    }

    @Test
    fun `applyTextStyle sets the text color, the size in sp and the system font`() {
        textView.applyTextStyle(resolvedStyle, sizeSp = 14f)

        assertEquals(Color.RED, textView.currentTextColor)
        val expectedSizePx =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizePx, textView.textSize, DELTA)
        assertEquals(Typeface.DEFAULT, textView.typeface)
    }

    @Test
    fun `applyTextStyle uses the medium system font for medium text`() {
        textView.applyTextStyle(resolvedStyle, sizeSp = 20f, weight = TextWeight.MEDIUM)

        assertEquals(Typeface.create("sans-serif-medium", Typeface.NORMAL), textView.typeface)
    }

    @Test
    fun `applyTextStyle uses the merchant's font as is for regular text`() {
        val font = givenMerchantFont()

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 14f)

        assertSame(font, textView.typeface)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `applyTextStyle uses the merchant's font at medium weight on Android 9 and later`() {
        val font = givenMerchantFont()

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 20f, weight = TextWeight.MEDIUM)

        assertNotEquals(font, textView.typeface)
        assertEquals(500, textView.typeface.weight)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    fun `applyTextStyle keeps the merchant's font for medium text before Android 9`() {
        val font = givenMerchantFont()

        textView.applyTextStyle(merchantFontStyle(), sizeSp = 20f, weight = TextWeight.MEDIUM)

        assertSame(font, textView.typeface)
    }

    @Test
    fun `applyTextStyle falls back to the system font when the font resource isn't a font`() {
        val notAFont = styleWith(PayPalSavedPaymentMethodAppearance(fontResId = R.drawable.ic_edit_pencil))

        textView.applyTextStyle(notAFont, sizeSp = 14f)

        assertEquals(Typeface.DEFAULT, textView.typeface)
    }

    @Test
    fun `applyTextStyle uses the given text color instead of the style's`() {
        textView.applyTextStyle(resolvedStyle, sizeSp = 14f, textColor = Color.BLUE)

        assertEquals(Color.BLUE, textView.currentTextColor)
    }

    private fun givenMerchantFont(): Typeface {
        val font = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        mockkStatic(ResourcesCompat::class)
        every { ResourcesCompat.getFont(any(), FONT_RES) } returns font
        return font
    }

    private fun merchantFontStyle() = styleWith(PayPalSavedPaymentMethodAppearance(fontResId = FONT_RES))

    private fun styleWith(appearance: PayPalSavedPaymentMethodAppearance) = PayPalSavedPaymentMethodStyleResolver(
        context,
        PayPalSavedPaymentMethodViewStyle(componentAppearance = appearance)
    )

    private companion object {
        // Any resource ID: ResourcesCompat.getFont is mocked to return a font for it.
        const val FONT_RES = 0x7f0b0001
        const val DELTA = 0.01f
    }
}
