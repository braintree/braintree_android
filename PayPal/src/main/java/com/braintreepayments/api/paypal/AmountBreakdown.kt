package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * A recurring billing amount breakdown.
 *
 * This object can only used for the [PayPalCheckoutRequest] to customize how the transaction amount is
 * broken down. If [AmountBreakdown] is provided, [itemTotal] is required. Some fields are conditionally required or
 * not accepted depending on the checkout flow (e.g., one-time vs subscription).
 *
 * @property itemTotal - Total amount of the items before any taxes or discounts.
 *
 * @property taxTotal - (optional) Total tax amount applied to the transaction.
 *                      Required if [lineItems.taxAmount] is provided. Should match the sum of tax
 *                      amounts from all line items.
 *
 * @property shippingTotal - (optional) Cost of shipping the items.
 *
 * @property handlingTotal - (optional) Cost associated with handling the items
 *                      (e.g., packaging or processing). Not accepted if
 *                      [PayPalRecurringBillingDetails] are included.
 *
 * @property insuranceTotal - (optional) Cost of insurance applied to the shipment or items.
 *                       Not accepted if [PayPalRecurringBillingDetails] are included.
 *
 * @property shippingDiscount - (optional) Discount amount applied specifically to shipping.
 *                               Not accepted if [PayPalRecurringBillingDetails] are included.
 *
 * @property discountTotal - (optional) General discount applied to the total transaction.
 *                      Not accepted if [PayPalRecurringBillingDetails] are included.
 */

@Parcelize
data class AmountBreakdown(
    val itemTotal: String,
    val taxTotal: String?,
    val shippingTotal: String?,
    val handlingTotal: String?,
    val insuranceTotal: String?,
    val shippingDiscount: String?,
    val discountTotal: String?
) : Parcelable {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_ITEM_TOTAL, itemTotal)
            putOpt(KEY_SHIPPING, shippingTotal)
            putOpt(KEY_HANDLING, handlingTotal)
            putOpt(KEY_TAX_TOTAL, taxTotal)
            putOpt(KEY_INSURANCE, insuranceTotal)
            putOpt(KEY_SHIPPING_DISCOUNT, shippingDiscount)
            putOpt(KEY_DISCOUNT, discountTotal)
        }
    }

    companion object {
        private const val KEY_ITEM_TOTAL = "item_total"
        private const val KEY_TAX_TOTAL = "tax_total"
        private const val KEY_SHIPPING = "shipping"
        private const val KEY_HANDLING = "handling"
        private const val KEY_INSURANCE = "insurance"
        private const val KEY_SHIPPING_DISCOUNT = "shipping_discount"
        private const val KEY_DISCOUNT = "discount"
    }
}
