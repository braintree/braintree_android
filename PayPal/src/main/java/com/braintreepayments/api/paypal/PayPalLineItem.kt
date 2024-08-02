package com.braintreepayments.api.paypal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Line item for PayPal checkout flows.
 *
 * @property kind Indicates whether the line item is a debit (sale) or credit (refund) to the
 * customer.
 * @property name Item name to display. Maximum 127 characters.
 * @property quantity Number of units of the item purchased. This value must be a whole number and
 * can't be negative or zero.
 * @property unitAmount Per-unit price of the item. Can include up to 2 decimal places. This value
 * can't be negative or zero.
 * @property description Item description to display. Maximum 127 characters.
 * @property imageUrl The image URL to product information.
 * @property productCode Product or UPC code for the item. Maximum 127 characters.
 * @property unitTaxAmount Per-unit tax price of the item. Can include up to 2 decimal places.
 * This value can't be negative or zero.
 * @property upcCode UPC code of the item.
 * @property upcType UPC type of the item.
 * @property url The URL to product information.
 */

@Parcelize
data class PayPalLineItem
@JvmOverloads constructor(
    val kind: PayPalLineItemKind,
    val name: String,
    val quantity: String,
    val unitAmount: String,
    var description: String? = null,
    var imageUrl: String? = null,
    var productCode: String? = null,
    var unitTaxAmount: String? = null,
    var upcCode: String? = null,
    var upcType: PayPalLineItemUpcType? = null,
    var url: String? = null
) : Parcelable {

    fun toJson(): JSONObject {
        return try {
            JSONObject()
                .putOpt(DESCRIPTION_KEY, description)
                .putOpt(IMAGE_URL_KEY, imageUrl)
                .putOpt(KIND_KEY, kind.stringValue)
                .putOpt(NAME_KEY, name)
                .putOpt(PRODUCT_CODE_KEY, productCode)
                .putOpt(QUANTITY_KEY, quantity)
                .putOpt(UNIT_AMOUNT_KEY, unitAmount)
                .putOpt(UNIT_TAX_AMOUNT_KEY, unitTaxAmount)
                .putOpt(UPC_CODE_KEY, upcCode)
                .putOpt(UPC_TYPE_KEY, upcType?.stringValue)
                .putOpt(URL_KEY, url)
        } catch (ignored: JSONException) {
            JSONObject()
        }
    }

    companion object {
        private const val DESCRIPTION_KEY = "description"
        private const val IMAGE_URL_KEY = "image_url"
        private const val KIND_KEY = "kind"
        private const val NAME_KEY = "name"
        private const val PRODUCT_CODE_KEY = "product_code"
        private const val QUANTITY_KEY = "quantity"
        private const val UNIT_AMOUNT_KEY = "unit_amount"
        private const val UNIT_TAX_AMOUNT_KEY = "unit_tax_amount"
        private const val UPC_CODE_KEY = "upc_code"
        private const val UPC_TYPE_KEY = "upc_type"
        private const val URL_KEY = "url"
    }
}
