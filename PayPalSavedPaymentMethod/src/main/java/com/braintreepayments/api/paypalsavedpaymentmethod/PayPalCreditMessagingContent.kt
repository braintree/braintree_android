package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Pay Later / Credit presentment messaging for the edit-FI row, flattened and ready to render.
 * Assembled by the client from [PayPalCreditMessagingUtils] so both the classic View and Compose
 * UI consume the same shape.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property message Copy to render, built from `content.main_items` and `content.disclaimer_items`.
 * Image blocks (e.g. the PayPal logo) contribute their `alternative_text` instead of being dropped.
 * @property learnMoreText Display text for the "Learn more" action, or empty if none was returned.
 * @property learnMoreUrl URL to open when "Learn more" is tapped, or empty if none was returned.
 */
@ExperimentalBetaApi
data class PayPalCreditMessagingContent(
    val message: String,
    val learnMoreText: String,
    val learnMoreUrl: String
)