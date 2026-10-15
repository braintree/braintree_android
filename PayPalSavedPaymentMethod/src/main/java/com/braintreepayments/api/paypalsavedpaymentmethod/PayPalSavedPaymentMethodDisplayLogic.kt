package com.braintreepayments.api.paypalsavedpaymentmethod

import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType

/**
 * The generic icon for a card or bank funding instrument, or `null` for types that render their label only, such as
 * PayPal Credit. Shared by the XML and Compose views.
 */
@DrawableRes
internal fun PayPalSavedPaymentMethodType?.iconRes(): Int? = when (this) {
    PayPalSavedPaymentMethodType.CARD -> R.drawable.ic_fi_card_placeholder
    PayPalSavedPaymentMethodType.BANK -> R.drawable.ic_fi_bank_placeholder
    PayPalSavedPaymentMethodType.PAYPAL_CREDIT, null -> null
}

/**
 * The last digits to show masked, or `null` when the label should be shown instead. Only cards and banks have real
 * last digits; PayPal Credit can come back with a placeholder such as `"0000"`.
 */
internal fun PayPalSavedPaymentMethod.maskedLastDigitsOrNull(): String? =
    lastDigits?.takeIf { type.iconRes() != null && it.isNotBlank() }

/**
 * The last digits separated by spaces, so screen readers read each digit instead of one number, or `null` when the
 * label is read on its own. Shared by the XML and Compose views.
 */
internal fun PayPalSavedPaymentMethod.spokenLastDigitsOrNull(): String? =
    maskedLastDigitsOrNull()?.toList()?.joinToString(" ")

/**
 * What screen readers read for the funding instrument chip: for a card or bank, the label and spaced last digits
 * formatted by [endingIn], for example "Visa, ending in 1 2 3 4"; otherwise the label or the buyer's email. Shared by
 * the XML and Compose views.
 */
internal fun PayPalSavedPaymentMethodDisplayState.accessibilityLabel(
    endingIn: (label: String, spokenDigits: String) -> String
): String = when (this) {
    is PayPalSavedPaymentMethodDisplayState.Content ->
        paymentMethod.spokenLastDigitsOrNull()?.let { endingIn(paymentMethod.label, it) } ?: paymentMethod.label
    is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> payerEmail
    PayPalSavedPaymentMethodDisplayState.Loading,
    PayPalSavedPaymentMethodDisplayState.NoNetwork,
    PayPalSavedPaymentMethodDisplayState.Error,
    PayPalSavedPaymentMethodDisplayState.Hidden -> ""
}

/**
 * Whether the funding instrument row is shown. It is hidden when the fetch failed, leaving only the PayPal logo and
 * label, and when the whole component is hidden.
 */
internal val PayPalSavedPaymentMethodDisplayState.showsFundingInstrumentRow: Boolean
    get() = this !is PayPalSavedPaymentMethodDisplayState.NoNetwork &&
        this !is PayPalSavedPaymentMethodDisplayState.Error &&
        this !is PayPalSavedPaymentMethodDisplayState.Hidden

/**
 * Whether the buyer can change the funding instrument, which shows the edit icon and makes the chip tappable. Always
 * `true` for a funding instrument; for an email, only when PayPal says the buyer can edit. Shared by the XML and
 * Compose views.
 */
internal val PayPalSavedPaymentMethodDisplayState.isEditable: Boolean
    get() = when (this) {
        is PayPalSavedPaymentMethodDisplayState.Content -> true
        is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument -> isEditable
        PayPalSavedPaymentMethodDisplayState.Loading,
        PayPalSavedPaymentMethodDisplayState.NoNetwork,
        PayPalSavedPaymentMethodDisplayState.Error,
        PayPalSavedPaymentMethodDisplayState.Hidden -> false
    }

/**
 * What the Pay Later row renders next to [fundingInstrumentState]. The offer is quoted for the funding instrument
 * fetched before any edit and only helps if the buyer can change it, so the row shows while that funding instrument is
 * loading or shown, and next to an email only when the buyer can edit. It is hidden next to the PayPal logo alone and
 * after an edit. Its shimmer only shows while the funding instrument is loading, so the row never shimmers on its own.
 * Shared by the XML and Compose views.
 */
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
