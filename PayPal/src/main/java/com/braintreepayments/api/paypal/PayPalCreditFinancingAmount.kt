package com.braintreepayments.api.paypal

import android.os.Parcel
import android.os.Parcelable
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * The currency and amount in a PayPal credit financing response
 *
 * @property currency 3 letter currency code as defined by
 * [ISO 4217](http://www.iso.org/iso/home/standards/currency_codes.htm).
 * @property value An amount defined by
 * [ISO 4217](http://www.iso.org/iso/home/standards/currency_codes.htm) for the given currency.
 */
@Parcelize
data class PayPalCreditFinancingAmount(
    val currency: String?,
    val value: String?,
) : Parcelable {

    companion object {
        private const val CURRENCY_KEY = "currency"
        private const val VALUE_KEY = "value"

        @JvmStatic
        fun fromJson(amount: JSONObject?): PayPalCreditFinancingAmount {
            return PayPalCreditFinancingAmount(
                currency = Json.optString(amount, CURRENCY_KEY, null),
                value = Json.optString(amount, VALUE_KEY, null)
            )
        }
    }
}
