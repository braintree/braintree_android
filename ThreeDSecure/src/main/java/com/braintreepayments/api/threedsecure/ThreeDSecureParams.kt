package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Data class to parse and contain 3D Secure authentication responses
 */
@Parcelize
data class ThreeDSecureParams(
    var threeDSecureNonce: ThreeDSecureNonce? = null,
    val errorMessage: String? = null,
    val lookup: ThreeDSecureLookup? = null
) : Parcelable {

    fun hasError(): Boolean {
        return errorMessage?.isNotEmpty() == true
    }

    companion object {
        private const val ERRORS_KEY = "errors"
        private const val ERROR_KEY = "error"
        private const val MESSAGE_KEY = "message"
        private const val PAYMENT_METHOD_KEY = "paymentMethod"
        private const val LOOKUP_KEY = "lookup"

        /**
         * Used to parse a response from the Braintree Gateway to be used for 3D Secure.*
         * @param jsonString The json response from the Braintree Gateway 3D Secure authentication
         *                   route.
         * @return The [ThreeDSecureParams] to use when performing 3D Secure authentication.
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(jsonString: String): ThreeDSecureParams {
            val json = JSONObject(jsonString)

            val cardNonce = json.optJSONObject(PAYMENT_METHOD_KEY)?.let {
                ThreeDSecureNonce.fromJSON(it)
            }

            val errorMessage = when {
                json.has(ERRORS_KEY) -> {
                    // 3DS v2
                    Json.optString(json.getJSONArray(ERRORS_KEY).getJSONObject(0), MESSAGE_KEY, null)
                }
                json.has(ERROR_KEY) -> {
                    // 3DS v1
                    Json.optString(json.getJSONObject(ERROR_KEY), MESSAGE_KEY, null)
                }
                else -> null
            }

            val lookup = json.optJSONObject(LOOKUP_KEY)?.let {
                ThreeDSecureLookup.fromJson(it.toString())
            }

            return ThreeDSecureParams(cardNonce, errorMessage, lookup)
        }
    }
}
