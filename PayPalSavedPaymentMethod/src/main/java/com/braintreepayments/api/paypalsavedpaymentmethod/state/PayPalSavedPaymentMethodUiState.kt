package com.braintreepayments.api.paypalsavedpaymentmethod.state

/** What the component shows, and whether an edit is running or completed. */
internal data class PayPalSavedPaymentMethodUiState(
    val fundingInstrument: PayPalSavedPaymentMethodDisplayState = PayPalSavedPaymentMethodDisplayState.Loading,
    val creditMessaging: PayPalCreditMessagingDisplayState = PayPalCreditMessagingDisplayState.Loading,
    val isEditing: Boolean = false,
    val isEditCompleted: Boolean = false
)
