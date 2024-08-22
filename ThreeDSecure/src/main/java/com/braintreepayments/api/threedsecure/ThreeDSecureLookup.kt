package com.braintreepayments.api.threedsecure

import android.os.Parcelable
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Data class to parse and contain 3D Secure lookup params
 */
@Parcelize
data class ThreeDSecureLookup internal constructor(
    val acsUrl: String?,
    val md: String,
    val termUrl: String,
    val pareq: String,
    val threeDSecureVersion: String,
    val transactionId: String
) : Parcelable {

    companion object {
        private const val ACS_URL_KEY = "acsUrl"
        private const val MD_KEY = "md"
        private const val TERM_URL_KEY = "termUrl"
        private const val PA_REQ_KEY = "pareq"
        private const val THREE_D_SECURE_VERSION_KEY = "threeDSecureVersion"
        private const val TRANSACTION_ID_KEY = "transactionId"

        /**
         * Used to parse a response from the Braintree Gateway to be used for 3D Secure.
         *
         * @param jsonString The json response from the Braintree Gateway 3D Secure lookup route.
         * @return The [ThreeDSecureLookup] to use when performing 3D Secure authentication.
         * @throws JSONException when parsing fails.
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(jsonString: String): ThreeDSecureLookup {
            val json = JSONObject(jsonString)
            val pareq = json.optString(PA_REQ_KEY, "")
            return ThreeDSecureLookup(
                acsUrl = Json.optString(json, ACS_URL_KEY, null),
                md = json.getString(MD_KEY),
                termUrl = json.getString(TERM_URL_KEY),
                pareq = if (pareq == "null") "" else pareq,
                threeDSecureVersion = json.getString(THREE_D_SECURE_VERSION_KEY),
                transactionId = json.getString(TRANSACTION_ID_KEY)
            )
        }
    }

    /**
     * @return true if 3D Secure user authentication is required, false otherwise.
     * This typically indicates that the ACS URL is present and a challenge flow is needed.
     */
    fun requiresUserAuthentication(): Boolean {
        return acsUrl != null
    }
}
