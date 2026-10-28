package com.braintreepayments.api.paypalsavedpaymentmethod.state

import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType

// Like iOS: PayPal products that come as a card or bank (e.g. PayPal Debit) carry a subtype and show their label.
private val PayPalSavedPaymentMethod.showsLastDigits: Boolean
    get() = (type == PayPalSavedPaymentMethodType.CARD || type == PayPalSavedPaymentMethodType.BANK) &&
        subtype == null && !lastDigits.isNullOrBlank()

/** The generic card or bank icon, or `null` when the label is shown instead. */
@DrawableRes
internal fun PayPalSavedPaymentMethod.iconRes(): Int? = when {
    !showsLastDigits -> null
    type == PayPalSavedPaymentMethodType.BANK -> R.drawable.ic_fi_bank_placeholder
    else -> R.drawable.ic_fi_card_placeholder
}

/** The last digits of a card or bank, or `null` to show the label instead. */
internal fun PayPalSavedPaymentMethod.lastDigitsOrNull(): String? = lastDigits.takeIf { showsLastDigits }

/** The last digits separated by spaces, so screen readers read each digit. */
internal fun PayPalSavedPaymentMethod.spokenLastDigitsOrNull(): String? =
    lastDigitsOrNull()?.toList()?.joinToString(" ")

/**
 * What screen readers read for the funding instrument chip, or `null` for states that show no chip. [brandName] is
 * "PayPal" when the logo and label are both hidden, otherwise `null`.
 */
internal fun PayPalSavedPaymentMethodDisplayState.accessibilityLabel(
    brandName: String?,
    endingIn: (label: String, spokenDigits: String) -> String
): String? = when (this) {
    is PayPalSavedPaymentMethodDisplayState.Content -> {
        val description =
            paymentMethod.spokenLastDigitsOrNull()?.let { endingIn(paymentMethod.label, it) } ?: paymentMethod.label
        listOfNotNull(brandName, description).joinToString(", ")
    }
    is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> payerEmail
    PayPalSavedPaymentMethodDisplayState.Loading,
    PayPalSavedPaymentMethodDisplayState.NoNetwork,
    PayPalSavedPaymentMethodDisplayState.Error,
    PayPalSavedPaymentMethodDisplayState.Hidden -> null
}

/** Whether the funding instrument row is shown. */
internal val PayPalSavedPaymentMethodDisplayState.showsFundingInstrumentRow: Boolean
    get() = this !is PayPalSavedPaymentMethodDisplayState.NoNetwork &&
        this !is PayPalSavedPaymentMethodDisplayState.Error &&
        this !is PayPalSavedPaymentMethodDisplayState.Hidden

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
