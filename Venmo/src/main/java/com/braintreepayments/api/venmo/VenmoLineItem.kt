package com.braintreepayments.api.venmo

import android.os.Parcelable
import androidx.annotation.StringDef
import com.braintreepayments.api.venmo.VenmoLineItem.VenmoLineItemKind
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
    @VenmoLineItemKind val kind: String,
    val name: String,
    val quantity: Int,
    val unitAmount: String,
    var description: String? = null,
    var productCode: String? = null,
    var unitTaxAmount: String? = null,
    var url: String? = null
) : Parcelable {
    /**
     * The type of Venmo line item.
     *
     * [.KIND_CREDIT] A line item that is a credit. [.KIND_DEBIT] A line item that
     * debits.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(KIND_CREDIT, KIND_DEBIT)
    internal annotation class VenmoLineItemKind

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
        const val KIND_CREDIT: String = "CREDIT"
        const val KIND_DEBIT: String = "DEBIT"

        internal const val DESCRIPTION_KEY = "description"
        const val KIND_KEY = "type"
        const val NAME_KEY = "name"
        const val PRODUCT_CODE_KEY = "productCode"
        const val QUANTITY_KEY = "quantity"
        const val UNIT_AMOUNT_KEY = "unitAmount"
        const val UNIT_TAX_AMOUNT_KEY = "unitTaxAmount"
        const val URL_KEY = "url"
    }
}
