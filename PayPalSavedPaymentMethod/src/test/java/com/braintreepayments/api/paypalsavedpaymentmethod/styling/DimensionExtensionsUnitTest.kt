package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DimensionExtensionsUnitTest {

    private val resources: Resources get() = RuntimeEnvironment.getApplication().resources

    @Test
    fun `pxToDp converts pixels to dp using the display density`() {
        assertEquals(16f, resources.pxToDp(16f * resources.displayMetrics.density), DELTA)
    }

    @Test
    fun `pxToSp converts pixels to sp with linear font scaling below Android 14`() {
        RuntimeEnvironment.setFontScale(LARGE_FONT_SCALE)

        @Suppress("DEPRECATION")
        val px = 20f * resources.displayMetrics.scaledDensity

        assertEquals(20f, resources.pxToSp(px), DELTA)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `pxToSp converts pixels to sp with non-linear font scaling on Android 14 and above`() {
        RuntimeEnvironment.setFontScale(LARGE_FONT_SCALE)
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)

        assertEquals(20f, resources.pxToSp(px), DELTA)
    }

    @Test
    fun `dimenDp reads a dp dimension resource in dp`() {
        assertEquals(10f, resources.dimenDp(R.dimen.paypal_saved_payment_method_style_default_vertical_padding), DELTA)
    }

    @Test
    fun `dimenSp reads an sp dimension resource in sp`() {
        assertEquals(20f, resources.dimenSp(R.dimen.paypal_saved_payment_method_style_default_label_font_size), DELTA)
    }

    private companion object {
        const val DELTA = 0.001f
        const val LARGE_FONT_SCALE = 2f
    }
}
