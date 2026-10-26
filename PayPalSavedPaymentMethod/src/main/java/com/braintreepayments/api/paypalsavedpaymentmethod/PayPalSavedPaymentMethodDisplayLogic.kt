package com.braintreepayments.api.paypalsavedpaymentmethod

import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType

/** The generic card or bank icon, or `null` for types shown by their label only. */
@DrawableRes
internal fun PayPalSavedPaymentMethodType?.iconRes(): Int? = when (this) {
    PayPalSavedPaymentMethodType.CARD -> R.drawable.ic_fi_card_placeholder
    PayPalSavedPaymentMethodType.BANK -> R.drawable.ic_fi_bank_placeholder
    PayPalSavedPaymentMethodType.PAYPAL_CREDIT, null -> null
}

/** The last digits of a card or bank, or `null` to show the label instead. */
internal fun PayPalSavedPaymentMethod.maskedLastDigitsOrNull(): String? =
    lastDigits?.takeIf { type.iconRes() != null && it.isNotBlank() }

/** The last digits separated by spaces, so screen readers read each digit. */
internal fun PayPalSavedPaymentMethod.spokenLastDigitsOrNull(): String? =
    maskedLastDigitsOrNull()?.toList()?.joinToString(" ")

/**
 * What screen readers read for the funding instrument chip. [brandName] is "PayPal" when the logo and label are both
 * hidden, otherwise `null`.
 */
internal fun PayPalSavedPaymentMethodDisplayState.accessibilityLabel(
    brandName: String?,
    endingIn: (label: String, spokenDigits: String) -> String
): String = when (this) {
    is PayPalSavedPaymentMethodDisplayState.Content -> {
        val description =
            paymentMethod.spokenLastDigitsOrNull()?.let { endingIn(paymentMethod.label, it) } ?: paymentMethod.label
        listOfNotNull(brandName, description).joinToString(", ")
    }
    is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> payerEmail
    PayPalSavedPaymentMethodDisplayState.Loading,
    PayPalSavedPaymentMethodDisplayState.NoNetwork,
    PayPalSavedPaymentMethodDisplayState.Error,
    PayPalSavedPaymentMethodDisplayState.Hidden -> ""
}

/** Whether the funding instrument row is shown. */
internal val PayPalSavedPaymentMethodDisplayState.showsFundingInstrumentRow: Boolean
    get() = this !is PayPalSavedPaymentMethodDisplayState.NoNetwork &&
        this !is PayPalSavedPaymentMethodDisplayState.Error &&
        this !is PayPalSavedPaymentMethodDisplayState.Hidden

/** Whether the buyer can change the funding instrument. */
internal val PayPalSavedPaymentMethodDisplayState.isEditable: Boolean
    get() = when (this) {
        is PayPalSavedPaymentMethodDisplayState.Content -> true
        is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> isEditable
        PayPalSavedPaymentMethodDisplayState.Loading,
        PayPalSavedPaymentMethodDisplayState.NoNetwork,
        PayPalSavedPaymentMethodDisplayState.Error,
        PayPalSavedPaymentMethodDisplayState.Hidden -> false
    }

/** What the Pay Later row shows next to [fundingInstrumentState]. It's hidden after an edit. */
internal fun PayPalCreditMessagingDisplayState.visibleWith(
    fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
    isEditCompleted: Boolean
): PayPalCreditMessagingDisplayState {
    val showsRow = !isEditCompleted && when (fundingInstrumentState) {
        PayPalSavedPaymentMethodDisplayState.Loading,
        is PayPalSavedPaymentMethodDisplayState.Content -> true
        is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> fundingInstrumentState.isEditable
        PayPalSavedPaymentMethodDisplayState.NoNetwork,
        PayPalSavedPaymentMethodDisplayState.Error,
        PayPalSavedPaymentMethodDisplayState.Hidden -> false
    }
    val isLoadingOnItsOwn = this == PayPalCreditMessagingDisplayState.Loading &&
        fundingInstrumentState != PayPalSavedPaymentMethodDisplayState.Loading
    return if (showsRow && !isLoadingOnItsOwn) this else PayPalCreditMessagingDisplayState.Hidden
}
