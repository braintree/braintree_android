package com.braintreepayments.api.venmo

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Venmo line item for Venmo checkout flows.
 *
 * @property description - The description to display.
 * @property kind - The [VenmoLineItemKind] kind.
 * @property name - The name of the item to display.
 * @property productCode - Product or UPC code for the item. Maximum 127 characters.
 * @property quantity -The quantity of the item.
 * @property unitAmount - The unit amount.
 * @property unitTaxAmount - Per-unit price of the item. Can include up to 2 decimal places. This
 * value can't be negative or zero.
 * @property url - The URL to product information.
 */
@Parcelize
class VenmoLineItem @JvmOverloads constructor(
    val kind: VenmoLineItemKind,
    val name: String,
    val quantity: Int,
    val unitAmount: String,
    var description: String? = null,
    var productCode: String? = null,
    var unitTaxAmount: String? = null,
    var url: String? = null
) : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun toJson(): JSONObject {
        return try {
            JSONObject()
                .putOpt(DESCRIPTION_KEY, description)
                .putOpt(KIND_KEY, kind)
                .putOpt(NAME_KEY, name)
                .putOpt(PRODUCT_CODE_KEY, productCode)
                .putOpt(QUANTITY_KEY, quantity)
                .putOpt(UNIT_AMOUNT_KEY, unitAmount)
                .putOpt(UNIT_TAX_AMOUNT_KEY, unitTaxAmount)
                .putOpt(URL_KEY, url)
        } catch (ignored: JSONException) {
            JSONObject()
        }
    }

    companion object {
        internal const val DESCRIPTION_KEY = "description"
        internal const val KIND_KEY = "type"
        internal const val NAME_KEY = "name"
        internal const val PRODUCT_CODE_KEY = "productCode"
        internal const val QUANTITY_KEY = "quantity"
        internal const val UNIT_AMOUNT_KEY = "unitAmount"
        internal const val UNIT_TAX_AMOUNT_KEY = "unitTaxAmount"
        internal const val URL_KEY = "url"
    }
}
