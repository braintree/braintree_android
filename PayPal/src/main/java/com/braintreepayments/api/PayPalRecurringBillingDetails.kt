package com.braintreepayments.api

/**
 * PayPal recurring billing product details
 *
 * @property billingCycles A list of billing cycles for trial billing and regular billing. A plan can have at most two trial cycles and only one regular cycle.
 * @property currencyISOCode The three-character ISO-4217 currency code that identifies the currency.
 * @property productName The name of the plan to display at checkout.
 * @property oneTimeFeeAmount Price and currency for any one-time charges due at plan signup.
 * @property productDescription Product description to display at the checkout.
 * @property productPrice
 * @property productQuantity Quantity associated with the product.
 * @property shippingAmount The shipping amount for the billing cycle at the time of checkout.
 * @property taxAmount The taxes for the billing cycle at the time of checkout.
 * @property totalAmount
 */
data class PayPalRecurringBillingDetails(
    var billingCycles: List<PayPalBillingCycle>,
    var currencyISOCode: String,
    var productName: String?,
    var oneTimeFeeAmount: String?,
    var productDescription: String?,
    var productPrice: String?,
    var productQuantity: Int?,
    var shippingAmount: String?,
    var taxAmount: String?,
    var totalAmount: String?
)
