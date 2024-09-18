package com.braintreepayments.api.sepadirectdebit

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethodNonce
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a SEPA Direct Debit payment.
 *
 * @see PaymentMethodNonce
 *
 * @property ibanLastFour The IBAN last four characters.
 * @property customerId The customer ID.
 * @property mandateType The [SEPADirectDebitMandateType].
 */
@Parcelize
class SEPADirectDebitNonce internal constructor(
    override val string: String,
    override val isDefault: Boolean,
    val ibanLastFour: String?,
    val customerId: String?,
    val mandateType: SEPADirectDebitMandateType?,
) : PaymentMethodNonce(
    string = string,
    isDefault = isDefault,
) {

    companion object {
        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val DETAILS_KEY = "details"
        private const val IBAN_LAST_FOUR_KEY = "ibanLastChars"
        private const val CUSTOMER_ID_KEY = "merchantOrPartnerCustomerId"
        private const val MANDATE_TYPE_KEY = "mandateType"

        @Throws(JSONException::class)
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromJSON(inputJson: JSONObject): SEPADirectDebitNonce {
            val nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY)
            val details = inputJson.optJSONObject(DETAILS_KEY)
            var ibanLastFour: String? = null
            var customerId: String? = null
            var mandateType: SEPADirectDebitMandateType? = null
            if (details != null) {
                ibanLastFour = details.optString(IBAN_LAST_FOUR_KEY)
                customerId = details.optString(CUSTOMER_ID_KEY)
                mandateType = SEPADirectDebitMandateType.valueOf(
                    details.optString(MANDATE_TYPE_KEY)
                )
            }

            return SEPADirectDebitNonce(
                string = nonce,
                isDefault = false,
                ibanLastFour = ibanLastFour,
                customerId = customerId,
                mandateType = mandateType
            )
        }
    }
}
