package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Every combination of funding instrument state, Pay Later state and edit, for [visibleWith]. */
@RunWith(TestParameterInjector::class)
class PayPalCreditMessagingVisibilityUnitTest {

    /** [allowsPayLater]: whether the Pay Later row may show next to this funding instrument state. */
    enum class FundingInstrument(
        internal val state: PayPalSavedPaymentMethodDisplayState,
        val allowsPayLater: Boolean
    ) {
        LOADING(PayPalSavedPaymentMethodDisplayState.Loading, allowsPayLater = true),
        CARD(PayPalSavedPaymentMethodDisplayState.Content(CARD_METHOD), allowsPayLater = true),
        EMAIL_EDITABLE(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(EMAIL, true), allowsPayLater = true),
        EMAIL_READ_ONLY(PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(EMAIL, false), allowsPayLater = false),
        NO_NETWORK(PayPalSavedPaymentMethodDisplayState.NoNetwork, allowsPayLater = false),
        ERROR(PayPalSavedPaymentMethodDisplayState.Error, allowsPayLater = false),
        HIDDEN(PayPalSavedPaymentMethodDisplayState.Hidden, allowsPayLater = false)
    }

    enum class PayLater(internal val state: PayPalCreditMessagingDisplayState) {
        LOADING(PayPalCreditMessagingDisplayState.Loading),
        MESSAGE(PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4", learnMore = null))),
        HIDDEN(PayPalCreditMessagingDisplayState.Hidden)
    }

    @Test
    fun `visibleWith covers every combination`(
        @TestParameter fundingInstrument: FundingInstrument,
        @TestParameter payLater: PayLater,
        @TestParameter isEditCompleted: Boolean
    ) {
        // The row shows only before an edit and next to a funding instrument that allows it. Its shimmer shows only
        // while the funding instrument is loading too, so the row never shimmers on its own.
        val expected = when {
            isEditCompleted || !fundingInstrument.allowsPayLater -> PayPalCreditMessagingDisplayState.Hidden
            payLater == PayLater.LOADING && fundingInstrument != FundingInstrument.LOADING ->
                PayPalCreditMessagingDisplayState.Hidden
            else -> payLater.state
        }

        assertEquals(expected, payLater.state.visibleWith(fundingInstrument.state, isEditCompleted))
    }

    private companion object {
        const val EMAIL = "buyer@example.com"
        val CARD_METHOD = PayPalSavedPaymentMethod(
            type = PayPalSavedPaymentMethodType.CARD,
            label = "Visa",
            imageUrl = null,
            lastDigits = "1234",
            subtype = null
        )
    }
}
