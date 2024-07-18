package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * PayPal Recurring Billing Agreement pricing details.
 *
 * @property pricingModel The pricing model associated with the billing agreement.
 * @property amount Price. The amount to charge for the subscription, recurring, UCOF or installments.
 * @property reloadThresholdAmount The reload trigger threshold condition amount when the customer is charged.
 */
@Parcelize
data class PayPalBillingPricing(
    var pricingModel: PayPalPricingModel,
    var amount: String,
    var reloadThresholdAmount: String?
) : Parcelable {

    constructor(pricingModel: PayPalPricingModel, amount: String) : this(pricingModel = pricingModel, amount = amount, reloadThresholdAmount = null)
    companion object {
        fun PayPalBillingPricing.toJson(): String {
            return JSONObject().apply {
                put(KEY_PRICING_MODEL, pricingModel.name)
                put(KEY_AMOUNT, amount)
                putOpt(KEY_RELOAD_THRESHOLD_AMOUNT, reloadThresholdAmount)
            }.toString()
        }

        private const val KEY_PRICING_MODEL = "pricing_model"
        private const val KEY_AMOUNT = "price"
        private const val KEY_RELOAD_THRESHOLD_AMOUNT = "reload_threshold_amount"
    }
}


