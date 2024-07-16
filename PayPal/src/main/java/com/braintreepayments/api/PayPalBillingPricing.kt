package com.braintreepayments.api

/**
 * PayPal Recurring Billing Agreement pricing details.
 *
 * @property pricingModel The pricing model associated with the billing agreement.
 * @property amount Price. The amount to charge for the subscription, recurring, UCOF or installments.
 * @property reloadThresholdAmount The reload trigger threshold condition amount when the customer is charged.
 */
data class PayPalBillingPricing(
    var pricingModel: PayPalPricingModel,
    var amount: String,
    var reloadThresholdAmount: String?
)


