package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.ExperimentalBetaApi

/**
 * Used to initialize a [PayPalMessagingView]
 * This feature is currently only supported for buyers located in the US. For merchants domiciled
 * outside of the US please set the [buyerCountry] to display messaging to US based buyers.
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property amount Price expressed in cents amount based on the current context
 * (i.e. individual product price vs total cart price)
 * @property pageType Message screen location (e.g. product, cart, home)
 * @property offerType Preferred message offer to display
 * @property buyerCountry Consumer's country (Integrations must be approved by PayPal to use this option)
 * @property logoType Logo type option for a PayPal Message. Defaults to [PayPalMessagingLogoType.INLINE]
 * @property textAlignment Text alignment option for a PayPal Message. Defaults to [PayPalMessagingTextAlignment.RIGHT]
 * @property color Text and logo color option for a PayPal Message. Defaults to [PayPalMessagingColor.BLACK]
 */
@ExperimentalBetaApi
data class PayPalMessagingRequest(
    var amount: Double? = null,
    var pageType: PayPalMessagingPageType? = null,
    var offerType: PayPalMessagingOfferType? = null,
    var buyerCountry: String? = null,
    var logoType: PayPalMessagingLogoType = PayPalMessagingLogoType.INLINE,
    var textAlignment: PayPalMessagingTextAlignment = PayPalMessagingTextAlignment.RIGHT,
    var color: PayPalMessagingColor = PayPalMessagingColor.BLACK
)
