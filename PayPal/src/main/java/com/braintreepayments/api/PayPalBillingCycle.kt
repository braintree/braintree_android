package com.braintreepayments.api

/**
 * PayPal recurring billing cycle details.
 *
 * @property interval The number of intervals after which a subscriber is charged or billed.
 * @property intervalCount The number of times this billing cycle gets executed. For example, if the [intervalCount] is [PayPalBillingInterval.DAY] with an [intervalCount] of 2, the subscription is billed once every two days. Maximum values [PayPalBillingInterval.DAY] -> 365, [PayPalBillingInterval.WEEK] -> 52, [PayPalBillingInterval.MONTH] -> 12, [PayPalBillingInterval.YEAR] -> 1.
 * @property numberOfExecutions The number of times this billing cycle gets executed. Trial billing cycles can only be executed a finite number of times (value between 1 and 999). Regular billing cycles can be executed infinite times (value of 0) or a finite number of times (value between 1 and 999).
 * @property sequence The sequence of the billing cycle. Used to identify unique billing cycles. For example, sequence 1 could be a 3 month trial period, and sequence 2 could be a longer term full rater cycle. Max value 100. All billing cycles should have unique sequence values.
 * @property startDate The date and time when the billing cycle starts, in Internet date and time format `YYYY-MM-DDT00:00:00Z`. If not provided the billing cycle starts at the time of checkout. If provided and the merchant wants the billing cycle to start at the time of checkout, provide the current time. Otherwise the [startDate] can be in future.
 * @property isTrial The tenure type of the billing cycle. In case of a plan having trial cycle, only 2 trial cycles are allowed per plan.
 * @property pricing The active pricing scheme for this billing cycle. Required if [isTrial] is false. Optional if [isTrial] is true.
 */
data class PayPalBillingCycle(
    var interval: PayPalBillingInterval,
    var intervalCount: Int,
    var numberOfExecutions: Int,
    var sequence: Int?,
    var startDate: String?,
    var isTrial: Boolean,
    var pricing: PayPalBillingPricing?
)
