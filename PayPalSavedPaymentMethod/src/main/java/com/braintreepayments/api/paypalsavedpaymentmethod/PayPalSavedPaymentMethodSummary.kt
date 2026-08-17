package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * A single vaulted funding instrument returned by the `paypalFundingInstrumentDetails` query.
 *
 * @property label      Display label, e.g. "CREDIT UNION 1".
 * @property imageUrl   Icon/logo URL served by the backend (the primary icon for every FI).
 * @property lastDigits Last digits of the instrument, when applicable.
 * @property type       Raw backend type: "CARD", "BANK", or "PAYPAL_CREDIT".
 * @property subtype    Raw backend subtype, present for "PAYPAL_CREDIT" (e.g. "PAY_LATER_US").
 */
@ExperimentalBetaApi
data class PayPalSavedpaymentMethod internal constructor(
    val label: String,
    val imageUrl: String,
    val lastDigits: String?,
    val type: String,
    val subtype: String?,
)

/**
 * The payer returned for a display-only (email-only) response.
 *
 * @property email    Payer email.
 * @property editable Whether the funding instrument is editable.
 */
@ExperimentalBetaApi
data class Payer internal constructor(
    val email: String,
    val editable: Boolean,
)

/**
 * The parsed result of a `paypalFundingInstrumentDetails` read.
 *
 * The payload is mutually exclusive: an instrument response carries [paypalSavedPaymentMethods] with
 * a null [paypalPayer]; a display-only response carries a [paypalPayer] with empty
 * [paypalSavedPaymentMethods]. An empty/absent payload yields empty [paypalSavedPaymentMethods] and a
 * null [paypalPayer] (the No-FI case).
 *
 * @property paypalPayer               Present for display-only responses; null when instruments are
 * returned.
 * @property paypalSavedPaymentMethods The vaulted funding instrument(s).
 */
@ExperimentalBetaApi
data class PayPalSavedPaymentMethodSummary internal constructor(
    val paypalPayer: Payer?,
    val paypalSavedPaymentMethods: List<PayPalSavedpaymentMethod>,
) {

    /**
     * The primary funding instrument (the first in [paypalSavedPaymentMethods]), or null for a
     * display-only / No-FI response.
     */
    val primaryInstrument: PayPalSavedpaymentMethod?
        get() = paypalSavedPaymentMethods.firstOrNull()

    internal companion object {

        private const val DATA_KEY = "data"
        private const val PAYPAL_FUNDING_INSTRUMENT_DETAILS_KEY = "paypalFundingInstrumentDetails"
        private const val PAYER_KEY = "payer"
        private const val PAYMENT_METHODS_KEY = "paymentMethods"
        private const val EMAIL_KEY = "email"
        private const val IS_EDITABLE_KEY = "editable"
        private const val LABEL_KEY = "label"
        private const val IMAGE_URL_KEY = "imageUrl"
        private const val LAST_DIGITS_KEY = "lastDigits"
        private const val TYPE_KEY = "type"
        private const val SUBTYPE_KEY = "subtype"

        fun fromJson(response: JSONObject): PayPalSavedPaymentMethodSummary {
            val payload = response
                .optJSONObject(DATA_KEY)
                ?.optJSONObject(PAYPAL_FUNDING_INSTRUMENT_DETAILS_KEY)

            val payer = payload?.optJSONObject(PAYER_KEY)?.let { payerJson ->
                Payer(
                    email = Json.optString(payerJson, EMAIL_KEY, ""),
                    editable = payerJson.optBoolean(IS_EDITABLE_KEY, false),
                )
            }

            val instruments = payload?.optJSONArray(PAYMENT_METHODS_KEY)?.let { array ->
                (0 until array.length()).map { index ->
                    val instrumentJson = array.getJSONObject(index)
                    PayPalSavedpaymentMethod(
                        label = Json.optString(instrumentJson, LABEL_KEY, ""),
                        imageUrl = Json.optString(instrumentJson, IMAGE_URL_KEY, ""),
                        lastDigits = Json.optString(instrumentJson, LAST_DIGITS_KEY, null),
                        type = Json.optString(instrumentJson, TYPE_KEY, ""),
                        subtype = Json.optString(instrumentJson, SUBTYPE_KEY, null),
                    )
                }
            }.orEmpty()

            return PayPalSavedPaymentMethodSummary(paypalPayer = payer, paypalSavedPaymentMethods = instruments)
        }
    }
}