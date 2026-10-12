package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.res.Resources
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DimensionExtensionsUnitTest {

    private val resources: Resources = RuntimeEnvironment.getApplication().resources
    private val density = resources.displayMetrics.density

    @Test
    fun `pxToDp converts pixels to dp using the display density`() {
        assertEquals(16f, resources.pxToDp(16f * density), DELTA)
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
    }
}
