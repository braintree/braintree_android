package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * A funding instrument PayPal can charge on the buyer's behalf.
 *
 * @property id PayPal's encrypted funding instrument ID, for example `"CC-…"` or `"BA-…"`.
 * @property rawType The `type` as returned by PayPal, or `null` when PayPal sends none. Sent to analytics, so types
 * this SDK version does not recognize are still reported.
 * @property label The display name of the funding instrument, for example `"Visa"` or `"CREDIT UNION 1"`.
 * @property imageUrl The card art or bank glyph to render alongside the label. `null` unless PayPal returns an https
 * URL, so the view can fall back to a generic glyph when no usable image is returned.
 * @property lastDigits The last digits of the funding instrument's account number.
 * @property subtype A further qualifier on [type], returned for [PayPalSavedPaymentMethodType.PAYPAL_CREDIT]
 * instruments.
 * @property cardBrand The card brand, for example `"VISA"`. Only set for cards PayPal can identify.
 */
internal data class PayPalSavedPaymentMethod(
    val rawType: String?,
    val label: String,
    val imageUrl: String?,
    val lastDigits: String?,
    val subtype: String?,
    val id: String? = null,
    val cardBrand: String? = null
) {

    /** The kind of funding instrument, or `null` when PayPal returns a type this SDK version does not recognize. */
    val type: PayPalSavedPaymentMethodType? get() = PayPalSavedPaymentMethodType.fromRawValue(rawType)

    companion object {
        private const val ID_KEY = "id"
        private const val TYPE_KEY = "type"
        private const val LABEL_KEY = "label"
        private const val IMAGE_URL_KEY = "imageUrl"
        private const val LAST_DIGITS_KEY = "lastDigits"
        private const val SUBTYPE_KEY = "subtype"
        private const val CARD_BRAND_KEY = "cardBrand"

        /**
         * Parses a single entry of the `paymentMethods` array. Returns `null` when the entry is not an object or has
         * a blank `label`, since there is nothing meaningful to display. An entry without a `type` is kept, as on iOS,
         * with a `null` [type].
         */
        fun fromJson(json: JSONObject?): PayPalSavedPaymentMethod? {
            if (json == null) return null
            val rawType = Json.optString(json, TYPE_KEY, null)
            val label = Json.optString(json, LABEL_KEY, null)
            if (label.isNullOrBlank()) return null
            return PayPalSavedPaymentMethod(
                rawType = rawType,
                label = label,
                imageUrl = Json.optString(json, IMAGE_URL_KEY, null).asHttpsUrlOrNull(),
                lastDigits = Json.optString(json, LAST_DIGITS_KEY, null),
                subtype = Json.optString(json, SUBTYPE_KEY, null),
                id = Json.optString(json, ID_KEY, null),
                cardBrand = Json.optString(json, CARD_BRAND_KEY, null)
            )
        }
    }
}
