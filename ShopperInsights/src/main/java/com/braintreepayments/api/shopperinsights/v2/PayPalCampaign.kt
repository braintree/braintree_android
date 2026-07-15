package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * A PayPal co-marketing campaign to associate with a customer session.
 *
 * @property id The PayPal-assigned campaign identifier.
 */
@ExperimentalBetaApi
data class PayPalCampaign(val id: String)
