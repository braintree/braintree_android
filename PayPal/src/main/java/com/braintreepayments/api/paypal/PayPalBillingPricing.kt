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
data class PayPalBillingPricing @JvmOverloads constructor(
    val pricingModel: PayPalPricingModel,
    val amount: String,
    var reloadThresholdAmount: String? = null
) : Parcelable {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_PRICING_MODEL, pricingModel.name)
            put(KEY_AMOUNT, amount)
            putOpt(KEY_RELOAD_THRESHOLD_AMOUNT, reloadThresholdAmount)
        }
    }

    companion object {

        private const val KEY_PRICING_MODEL = "pricing_model"
        private const val KEY_AMOUNT = "price"
        private const val KEY_RELOAD_THRESHOLD_AMOUNT = "reload_threshold_amount"
    }
}
