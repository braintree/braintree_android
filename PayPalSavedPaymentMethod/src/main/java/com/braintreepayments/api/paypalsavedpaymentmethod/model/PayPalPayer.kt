package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * The buyer's PayPal account.
 *
 * @property email The email address associated with the buyer's PayPal account.
 * @property isEditable Whether the buyer is allowed to change the funding instrument PayPal will charge. `false` when
 * PayPal does not say.
 */
internal data class PayPalPayer(
    val email: String,
    val isEditable: Boolean
) {

    companion object {
        private const val EMAIL_KEY = "email"
        private const val EDITABLE_KEY = "editable"

        /**
         * Parses the `payer` field of a funding instrument details response. Returns `null` when PayPal returns no
         * payer or the payer has a blank email, since there is nothing meaningful to display.
         */
        fun fromJson(json: JSONObject?): PayPalPayer? {
            if (json == null) return null
            val email = Json.optString(json, EMAIL_KEY, null)
            if (email.isNullOrBlank()) return null
            return PayPalPayer(
                email = email,
                isEditable = Json.optBoolean(json, EDITABLE_KEY, false)
            )
        }
    }
}
