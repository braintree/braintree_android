package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * PayPal recurring billing cycle details.
 *
 * @property isTrial The tenure type of the billing cycle. In case of a plan having trial cycle,
 * only 2 trial cycles are allowed per plan.
 * @property numberOfExecutions The number of times this billing cycle gets executed. Trial billing
 * cycles can only be executed a finite number of times (value between 1 and 999). Regular billing
 * cycles can be executed infinite times (value of 0) or a finite number of times (value between 1
 * and 999).
 * @property interval The number of intervals after which a subscriber is charged or billed.
 * @property intervalCount The number of times this billing cycle gets executed. For example, if
 * the [intervalCount] is [PayPalBillingInterval.DAY] with an [intervalCount] of 2, the subscription
 * is billed once every two days. Maximum values [PayPalBillingInterval.DAY] -> 365,
 * [PayPalBillingInterval.WEEK] -> 52, [PayPalBillingInterval.MONTH] -> 12,
 * [PayPalBillingInterval.YEAR] -> 1.
 * @property sequence The sequence of the billing cycle. Used to identify unique billing cycles. For
 * example, sequence 1 could be a 3 month trial period, and sequence 2 could be a longer term full
 * rater cycle. Max value 100. All billing cycles should have unique sequence values.
 * @property startDate The date and time when the billing cycle starts, in Internet date and time
 * format `YYYY-MM-DD`. If not provided the billing cycle starts at the time of checkout.
 * If provided and the merchant wants the billing cycle to start at the time of checkout, provide
 * the current time. Otherwise the [startDate] can be in future.
 * @property pricing The active pricing scheme for this billing cycle. Required if [isTrial] is
 * false. Optional if [isTrial] is true.
 */
@Parcelize
data class PayPalBillingCycle @JvmOverloads constructor(
    var isTrial: Boolean,
    val numberOfExecutions: Int,
    val interval: PayPalBillingInterval? = null,
    val intervalCount: Int? = null,
    var sequence: Int? = null,
    var startDate: String? = null,
    var pricing: PayPalBillingPricing? = null
) : Parcelable {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_TRIAL, isTrial)
            put(KEY_NUMBER_OF_EXECUTIONS, numberOfExecutions)
            putOpt(KEY_INTERVAL, interval)
            putOpt(KEY_INTERVAL_COUNT, intervalCount)
            putOpt(KEY_SEQUENCE, sequence)
            putOpt(KEY_START_DATE, startDate)
            pricing?.let {
                put(KEY_PRICING, it.toJson())
            }
        }
    }

    companion object {

        private const val KEY_INTERVAL = "billing_frequency_unit"
        private const val KEY_INTERVAL_COUNT = "billing_frequency"
        private const val KEY_NUMBER_OF_EXECUTIONS = "number_of_executions"
        private const val KEY_SEQUENCE = "sequence"
        private const val KEY_START_DATE = "start_date"
        private const val KEY_TRIAL = "trial"
        private const val KEY_PRICING = "pricing_scheme"
    }
}
