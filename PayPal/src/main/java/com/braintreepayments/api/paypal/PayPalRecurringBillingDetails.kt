package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * PayPal recurring billing product details
 *
 * @property totalAmount
 * @property billingCycles A list of billing cycles for trial billing and regular billing. A plan
 * can have at most two trial cycles and only one regular cycle.
 * @property currencyISOCode The three-character ISO-4217 currency code that identifies the
 * currency.
 * @property productName The name of the plan to display at checkout.
 * @property oneTimeFeeAmount Price and currency for any one-time charges due at plan signup.
 * @property productDescription Product description to display at the checkout.
 * @property productAmount The item price for the product associated with the billing cycle at the
 * time of checkout.
 * @property productQuantity Quantity associated with the product.
 * @property shippingAmount The shipping amount for the billing cycle at the time of checkout.
 * @property taxAmount The taxes for the billing cycle at the time of checkout.
 */
@Parcelize
data class PayPalRecurringBillingDetails @JvmOverloads constructor(
    val billingCycles: List<PayPalBillingCycle>,
    val totalAmount: String,
    val currencyISOCode: String,
    var productName: String? = null,
    var oneTimeFeeAmount: String? = null,
    var productDescription: String? = null,
    var productAmount: String? = null,
    var productQuantity: Int? = null,
    var shippingAmount: String? = null,
    var taxAmount: String? = null,
) : Parcelable {

    fun toJson(): String {
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

    companion object {

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
