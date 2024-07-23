package com.braintreepayments.api.paypal

import android.os.Parcelable
import com.braintreepayments.api.paypal.PayPalBillingCycle.Companion.toJson
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * PayPal recurring billing product details
 *
 * @property totalAmount
 * @property billingCycles A list of billing cycles for trial billing and regular billing. A plan can have at most two trial cycles and only one regular cycle.
 * @property currencyISOCode The three-character ISO-4217 currency code that identifies the currency.
 * @property productName The name of the plan to display at checkout.
 * @property oneTimeFeeAmount Price and currency for any one-time charges due at plan signup.
 * @property productDescription Product description to display at the checkout.
 * @property productAmount The item price for the product associated with the billing cycle at the time of checkout.
 * @property productQuantity Quantity associated with the product.
 * @property shippingAmount The shipping amount for the billing cycle at the time of checkout.
 * @property taxAmount The taxes for the billing cycle at the time of checkout.
 */
@Parcelize
data class PayPalRecurringBillingDetails(
    var billingCycles: List<PayPalBillingCycle>,
    var totalAmount: String,
    var currencyISOCode: String,
    var productName: String?,
    var oneTimeFeeAmount: String?,
    var productDescription: String?,
    var productAmount: String?,
    var productQuantity: Int?,
    var shippingAmount: String?,
    var taxAmount: String?,
) : Parcelable {

    constructor(billingCycles: List<PayPalBillingCycle>, totalAmount: String, currencyISOCode: String) : this(billingCycles, totalAmount, currencyISOCode, null, null, null, null, null, null, null)
    companion object {
        @JvmStatic
        fun PayPalRecurringBillingDetails.toJson(): String {
            return JSONObject().apply {
                put(KEY_BILLING_CYCLES, JSONArray().apply {
                    for (billingCycle in billingCycles) {
                        put(billingCycle.toJson())
                    }
                })
                put(KEY_TOTAL_AMOUNT, totalAmount)
                put(KEY_CURRENCY_ISO_CODE, currencyISOCode)
                putOpt(KEY_PRODUCT_NAME, productName)
                putOpt(KEY_ONE_TIME_FEE_AMOUNT, oneTimeFeeAmount)
                putOpt(KEY_PRODUCT_DESCRIPTION, productDescription)
                putOpt(KEY_PRODUCT_PRICE, productAmount)
                putOpt(KEY_PRODUCT_QUANTITY, productQuantity)
                putOpt(KEY_SHIPPING_AMOUNT, shippingAmount)
                putOpt(KEY_TAX_AMOUNT, taxAmount)
            }.toString()
        }

        private const val KEY_BILLING_CYCLES = "billing_cycles"
        private const val KEY_CURRENCY_ISO_CODE = "currency_iso_code"
        private const val KEY_PRODUCT_NAME = "name"
        private const val KEY_ONE_TIME_FEE_AMOUNT = "one_time_fee_amount"
        private const val KEY_PRODUCT_DESCRIPTION = "product_description"
        private const val KEY_PRODUCT_PRICE = "product_price"
        private const val KEY_PRODUCT_QUANTITY = "product_quantity"
        private const val KEY_SHIPPING_AMOUNT = "shipping_amount"
        private const val KEY_TAX_AMOUNT = "tax_amount"
        private const val KEY_TOTAL_AMOUNT = "total_amount"
    }
}
