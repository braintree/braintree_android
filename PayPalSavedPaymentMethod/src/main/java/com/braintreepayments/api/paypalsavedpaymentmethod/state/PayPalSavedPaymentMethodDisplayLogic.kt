package com.braintreepayments.api.paypalsavedpaymentmethod.state

import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType

// Card and bank products with a subtype (e.g. PayPal Debit) show their label, not the last digits.
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

/**
 * What the Pay Later row shows next to [fundingInstrumentState]; the two load as one unit. While the funding
 * instrument loads, the row shimmers and a message that arrives early is held back. Once the funding instrument is
 * shown, the row follows its own state (shimmer, message, or hidden if its fetch failed). If the funding instrument
 * fails, can't be edited, or after an edit, the row is hidden.
 */
internal fun PayPalCreditMessagingDisplayState.visibleWith(
    fundingInstrumentState: PayPalSavedPaymentMethodDisplayState,
    isEditCompleted: Boolean
): PayPalCreditMessagingDisplayState {
    if (isEditCompleted || this == PayPalCreditMessagingDisplayState.Hidden) {
        return PayPalCreditMessagingDisplayState.Hidden
    }
    return when (fundingInstrumentState) {
        PayPalSavedPaymentMethodDisplayState.Loading -> PayPalCreditMessagingDisplayState.Loading
        is PayPalSavedPaymentMethodDisplayState.Content -> this
        is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument ->
            if (fundingInstrumentState.isEditable) this else PayPalCreditMessagingDisplayState.Hidden
        PayPalSavedPaymentMethodDisplayState.NoNetwork,
        PayPalSavedPaymentMethodDisplayState.Error,
        PayPalSavedPaymentMethodDisplayState.Hidden -> PayPalCreditMessagingDisplayState.Hidden
    }
}
