package com.braintreepayments.api.paypal

import com.braintreepayments.api.sharedutils.Json
import org.json.JSONException
import org.json.JSONObject

/**
 * A PayPal payment resource
 *
 * @property redirectUrl The redirectUrl for the payment used by One Touch Core for authorization
 * containing an EC token
 */
internal data class PayPalPaymentResource(
    val redirectUrl: String
) {

    companion object {
        private const val PAYMENT_RESOURCE_KEY = "paymentResource"
        private const val REDIRECT_URL_KEY = "redirectUrl"
        private const val AGREEMENT_SETUP_KEY = "agreementSetup"
        private const val APPROVAL_URL_KEY = "approvalUrl"
        private const val PAYPAL_APP_APPROVAL_URL_KEY = "paypalAppApprovalUrl"

        /**
         * Create a PayPalPaymentResource from a jsonString. Checks for keys associated with Single
         * Payment and Billing Agreement flows.
         *
         * @param jsonString a valid JSON string representing the payment resource
         * @return a PayPal payment resource
         * @throws JSONException when json input is invalid
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(jsonString: String): PayPalPaymentResource {
            val json = JSONObject(jsonString)
            val paymentResource = json.optJSONObject(PAYMENT_RESOURCE_KEY)
            val redirectUrl = if (paymentResource != null) {
                Json.optString(paymentResource, REDIRECT_URL_KEY, "")
            } else {
                val redirectJson = json.optJSONObject(AGREEMENT_SETUP_KEY)
                val payPalApprovalURL = Json.optString(redirectJson, PAYPAL_APP_APPROVAL_URL_KEY, "")
                payPalApprovalURL.ifEmpty {
                    Json.optString(
                        redirectJson,
                        APPROVAL_URL_KEY,
                        ""
                    )
                }
            }
            return PayPalPaymentResource(redirectUrl)
        }
    }
}
