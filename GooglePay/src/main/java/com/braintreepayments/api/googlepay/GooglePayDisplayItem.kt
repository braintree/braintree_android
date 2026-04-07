package com.braintreepayments.api.googlepay

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A cart item shown in the payment sheet
 * (e.g. subtotals, sales taxes, shipping charges, discounts etc.).
 * This is typically populated in the payment sheet if you use Authorize Payments
 * or Dynamic Price Updates.
 * See: https://developers.google.com/pay/api/web/reference/request-objects#DisplayItem
 * @property label The label to be displayed for the given option.
 * @property type The type of the given option.
 * @property price The monetary value of the cart item with an optional decimal precision
 * of two decimal places. Negative values are allowed.
 * @property status The status of the cart item. Defines price variance
 */
@Parcelize
data class GooglePayDisplayItem(
    val label: String,
    val type: GooglePayDisplayItemType,
    val price: String,
    val status: GooglePayDisplayItemStatus = GooglePayDisplayItemStatus.FINAL
) : Parcelable {

    /**
     * Assembles a JSON object compatible with the Google Pay API
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun toJson(): JSONObject = JSONObject().apply {
        put("label", label)
        put("type", type.stringValue)
        put("price", price)
        put("status", status.stringValue)
    }
}
