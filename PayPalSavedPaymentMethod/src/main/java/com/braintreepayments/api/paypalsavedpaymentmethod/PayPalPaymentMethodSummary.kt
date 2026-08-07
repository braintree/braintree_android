package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * A single vaulted funding instrument returned by the `getSavedPaymentMethod` query.
 *
 * @property label      Display label, e.g. "CREDIT UNION 1".
 * @property imageUrl   Icon/logo URL served by the backend (the primary icon for every FI).
 * @property lastDigits Last digits of the instrument, when applicable.
 * @property type       Raw backend type: "CARD", "BANK", or "PAYPAL_CREDIT".
 * @property subtype    Raw backend subtype, present for "PAYPAL_CREDIT" (e.g. "PAY_LATER_US").
 */
@ExperimentalBetaApi
data class PayPalPaymentMethod internal constructor(
    val label: String?,
    val imageUrl: String?,
    val lastDigits: String?,
    val type: String?,
    val subtype: String?,
)

/**
 * The payer returned for a display-only (email-only) response.
 *
 * @property email      Payer email.
 * @property isEditable Whether the funding instrument is editable.
 */
@ExperimentalBetaApi
data class Payer internal constructor(
    val email: String?,
    val isEditable: Boolean?,
)

/**
 * The parsed result of a `getSavedPaymentMethod` read.
 *
 * The payload is mutually exclusive: an instrument response carries [paymentMethods] with a null
 * [payer]; a display-only response carries a [payer] with empty [paymentMethods]. An empty/absent
 * payload yields empty [paymentMethods] and a null [payer] (the No-FI case).
 *
 * @property paymentMethods The vaulted funding instrument(s).
 * @property payer          Present for display-only responses; null when instruments are returned.
 */
@ExperimentalBetaApi
data class PayPalPaymentMethodSummary internal constructor(
    val paymentMethods: List<PayPalPaymentMethod>,
    val payer: Payer?,
) {

    /**
     * The primary funding instrument (the first in [paymentMethods]), or null for a display-only /
     * No-FI response.
     */
    val primaryInstrument: PayPalPaymentMethod?
        get() = paymentMethods.firstOrNull()

    internal companion object {

        private const val DATA_KEY = "data"
        private const val GET_SAVED_PAYMENT_METHOD_KEY = "getSavedPaymentMethod"
        private const val PAYER_KEY = "payer"
        private const val PAYMENT_METHODS_KEY = "paymentMethods"
        private const val EMAIL_KEY = "email"
        private const val IS_EDITABLE_KEY = "isEditable"
        private const val LABEL_KEY = "label"
        private const val IMAGE_URL_KEY = "imageUrl"
        private const val LAST_DIGITS_KEY = "lastDigits"
        private const val TYPE_KEY = "type"
        private const val SUBTYPE_KEY = "subtype"

        fun fromJson(response: JSONObject): PayPalPaymentMethodSummary {
            val payload = response
                .optJSONObject(DATA_KEY)
                ?.optJSONObject(GET_SAVED_PAYMENT_METHOD_KEY)

            val payer = payload?.optJSONObject(PAYER_KEY)?.let { payerJson ->
                Payer(
                    email = Json.optString(payerJson, EMAIL_KEY, null),
                    isEditable = if (payerJson.isNull(IS_EDITABLE_KEY)) {
                        null
                    } else {
                        payerJson.optBoolean(IS_EDITABLE_KEY)
                    },
                )
            }

            val instruments = payload?.optJSONArray(PAYMENT_METHODS_KEY)?.let { array ->
                (0 until array.length()).map { index ->
                    val instrumentJson = array.getJSONObject(index)
                    PayPalPaymentMethod(
                        label = Json.optString(instrumentJson, LABEL_KEY, null),
                        imageUrl = Json.optString(instrumentJson, IMAGE_URL_KEY, null),
                        lastDigits = Json.optString(instrumentJson, LAST_DIGITS_KEY, null),
                        type = Json.optString(instrumentJson, TYPE_KEY, null),
                        subtype = Json.optString(instrumentJson, SUBTYPE_KEY, null),
                    )
                }
            }.orEmpty()

            return PayPalPaymentMethodSummary(paymentMethods = instruments, payer = payer)
        }
    }
}
