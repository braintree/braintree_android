package com.braintreepayments.api

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
