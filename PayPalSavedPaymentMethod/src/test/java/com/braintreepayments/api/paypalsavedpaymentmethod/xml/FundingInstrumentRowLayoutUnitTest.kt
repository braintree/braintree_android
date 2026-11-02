@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FundingInstrumentRowLayoutUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val content = PayPalSavedPaymentMethodContentView(context)
    private val row: FundingInstrumentRowLayout =
        content.findViewById(R.id.paypal_saved_payment_method_funding_instrument_row)
    private val brand: View = content.findViewById(R.id.paypal_saved_payment_method_brand)
    private val chip: View = content.findViewById(R.id.paypal_saved_payment_method_funding_instrument_chip)
    private val shimmer: View = content.findViewById(R.id.paypal_saved_payment_method_funding_instrument_shimmer)

    @Test
    fun `the funding instrument sits next to the logo and label when it fits`() {
        render(email("buyer@example.com"))
        layout(WIDE)

        assertFalse(row.isStacked)
        assertTrue(chip.left >= brand.right)
        assertTrue(abs((brand.top + brand.height / 2) - (chip.top + chip.height / 2)) <= 1)
    }

    @Test
    fun `the funding instrument goes below the logo and label when it doesn't fit`() {
        render(email("a.very.long.buyer.email.address@example.com"))
        layout(NARROW)

        assertTrue(row.isStacked)
        assertTrue(chip.top >= brand.bottom)
        assertEquals(0, chip.left)
        assertTrue(chip.right <= NARROW)
    }

    @Test
    fun `the loading shimmer never stacks and fills the rest of the row`() {
        render(PayPalSavedPaymentMethodDisplayState.Loading)
        layout(NARROW)

        assertFalse(row.isStacked)
        assertTrue(shimmer.left >= brand.right)
        assertEquals(NARROW, shimmer.right)
    }

    @Test
    fun `without the logo and label the funding instrument starts at the leading edge and never stacks`() {
        render(
            email("a.very.long.buyer.email.address@example.com"),
            PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false)
        )
        layout(NARROW)

        assertFalse(row.isStacked)
        assertEquals(0, chip.left)
    }

    @Test
    fun `right-to-left layouts start from the right edge`() {
        content.layoutDirection = View.LAYOUT_DIRECTION_RTL
        render(email("buyer@example.com"))
        layout(WIDE)

        assertEquals(WIDE, brand.right)
        assertTrue(chip.right <= brand.left)
    }

    private fun email(address: String) =
        PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(address, isEditable = true)

    private fun render(
        state: PayPalSavedPaymentMethodDisplayState,
        style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle()
    ) = content.render(
        fundingInstrumentState = state,
        creditMessagingState = PayPalCreditMessagingDisplayState.Hidden,
        resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style),
        editEnabled = true,
        isEditCompleted = false
    )

    private fun layout(width: Int) {
        content.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)
    }

    private companion object {
        const val WIDE = 2000
        const val NARROW = 300
    }
}
