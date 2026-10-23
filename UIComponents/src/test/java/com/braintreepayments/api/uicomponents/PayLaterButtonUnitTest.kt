package com.braintreepayments.api.uicomponents

import android.content.Context
import android.view.View.MeasureSpec
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayLaterButtonUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // region PayLaterButtonColor
    @Test
    fun `each color maps to PayPal styling with its own wordmark`() {
        listOf(
            Triple(PayLaterButtonColor.Blue, PayPalButtonColor.Blue, R.drawable.pay_later_logo_black),
            Triple(PayLaterButtonColor.Black, PayPalButtonColor.Black, R.drawable.pay_later_logo_white),
            Triple(PayLaterButtonColor.White, PayPalButtonColor.White, R.drawable.pay_later_logo_black),
        ).forEach { (payLater, payPal, logo) ->
            assertEquals(logo, payLater.logoId)
            assertEquals(payPal.toStyle().copy(logoId = logo), payLater.toStyle())
        }
    }

    @Test
    fun `fromId maps keys to colors and falls back to blue`() {
        assertEquals(PayLaterButtonColor.Blue, PayLaterButtonColor.fromId(0))
        assertEquals(PayLaterButtonColor.Black, PayLaterButtonColor.fromId(1))
        assertEquals(PayLaterButtonColor.White, PayLaterButtonColor.fromId(2))
        assertEquals(PayLaterButtonColor.Blue, PayLaterButtonColor.fromId(UNKNOWN_COLOR_KEY))
    }

    @Test
    fun `wordmark drawables load at 155dp by 27dp`() {
        val density = context.resources.displayMetrics.density
        listOf(R.drawable.pay_later_logo_black, R.drawable.pay_later_logo_white).forEach { id ->
            val drawable = ContextCompat.getDrawable(context, id)

            assertNotNull(drawable)
            assertEquals((LOGO_WIDTH_DP * density).toInt(), drawable!!.intrinsicWidth)
            assertEquals((LOGO_HEIGHT_DP * density).toInt(), drawable.intrinsicHeight)
        }
    }

    // endregion

    // region XML attribute

    @Test
    fun `defaults to blue when no color attribute is set`() {
        val button = PayLaterButton(context)

        assertEquals(PayLaterButtonColor.Blue, button.currentStyle)
    }

    @Test
    fun `paymentButtonColor black attribute sets black`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paymentButtonColor, "black")
            .build()

        val button = PayLaterButton(context, attrs)

        assertEquals(PayLaterButtonColor.Black, button.currentStyle)
    }

    @Test
    fun `paymentButtonColor white attribute sets white`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.paymentButtonColor, "white")
            .build()

        val button = PayLaterButton(context, attrs)

        assertEquals(PayLaterButtonColor.White, button.currentStyle)
    }

    @Test
    fun `setButtonColor updates the color`() {
        val button = PayLaterButton(context)

        button.setButtonColor(PayLaterButtonColor.Black)
        assertEquals(PayLaterButtonColor.Black, button.currentStyle)

        button.setButtonColor(PayLaterButtonColor.White)
        assertEquals(PayLaterButtonColor.White, button.currentStyle)
    }

    // endregion

    // region Accessibility

    @Test
    fun `contentDescription is PayPal Pay Later`() {
        val button = PayLaterButton(context)

        assertEquals("PayPal Pay Later", button.contentDescription.toString())
    }

    // endregion

    // region Request

    @Test
    fun `setPayPalRequest sets shouldOfferPayLater to true`() {
        val button = PayLaterButton(context)
        val request = PayPalCheckoutRequest("10.00", true)
        assertFalse(request.shouldOfferPayLater)

        button.setPayPalRequest(request)

        assertTrue(request.shouldOfferPayLater)
    }

    // endregion

    // region Size

    @Test
    fun `measures to the shared payment button height and width`() {
        val button = PayLaterButton(context)
        val spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        button.measure(spec, spec)

        assertEquals(dimen(R.dimen.pay_button_height), button.measuredHeight)
        assertEquals(maxOf(dimen(R.dimen.pay_button_width), minWidth()), button.measuredWidth)
    }

    @Test
    fun `width below minimum clamps to fit the wordmark`() {
        val button = PayLaterButton(context)
        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(dimen(R.dimen.pay_button_height), MeasureSpec.EXACTLY)

        button.measure(widthSpec, heightSpec)

        assertEquals(minWidth(), button.measuredWidth)
        assertTrue(button.measuredWidth > logo().intrinsicWidth)
    }

    // endregion

    private fun dimen(id: Int) = context.resources.getDimension(id).toInt()

    private fun logo() = ContextCompat.getDrawable(context, R.drawable.pay_later_logo_black)!!

    private fun minWidth() = logo().intrinsicWidth + 4 * dimen(R.dimen.pay_button_focus_padding)

    companion object {
        private const val UNKNOWN_COLOR_KEY = 99
        private const val LOGO_WIDTH_DP = 155
        private const val LOGO_HEIGHT_DP = 27
    }
}
