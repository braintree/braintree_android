package com.braintreepayments.api.paypalsavedpaymentmethod.state

/** What the component shows, and the edit it has completed. */
internal data class PayPalSavedPaymentMethodUiState(
    val fundingInstrument: PayPalSavedPaymentMethodDisplayState = PayPalSavedPaymentMethodDisplayState.Loading,
    val creditMessaging: PayPalCreditMessagingDisplayState = PayPalCreditMessagingDisplayState.Loading,
    val isEditing: Boolean = false,
    val isEditCompleted: Boolean = false,
    val editedOrderId: String? = null
)
