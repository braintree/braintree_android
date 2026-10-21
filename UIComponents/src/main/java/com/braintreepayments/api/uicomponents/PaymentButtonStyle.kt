package com.braintreepayments.api.uicomponents

internal data class PaymentButtonStyle(
    val logoId: Int,
    val spinnerId: Int,
    val default: PayPalButtonColor.ButtonColors,
    val hover: PayPalButtonColor.ButtonColors,
    val focus: PayPalButtonColor.ButtonColors,
    val focusHover: PayPalButtonColor.ButtonColors,
    val pressed: PayPalButtonColor.ButtonColors
)

internal fun PayPalButtonColor.toStyle() = PaymentButtonStyle(
    logoId = logoId,
    spinnerId = spinnerId,
    default = default,
    hover = hover,
    focus = focus,
    focusHover = focusHover,
    pressed = pressed
)