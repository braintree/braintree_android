package com.braintreepayments.api.card

import android.os.Parcelable
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 *
 * @property regulationEnvironment for the associated nonce to help determine the need for 3D Secure.
 *
 * @see <a href="https://developer.paypal.com/braintree/docs/guides/3d-secure/advanced-options/android/
 * v4#authentication-insight">Documentation</a> for possible values.
 */
@Parcelize
data class AuthenticationInsight(val regulationEnvironment: String) : Parcelable {

    companion object {

        private const val GRAPHQL_REGULATION_ENVIRONMENT_KEY = "customerAuthenticationRegulationEnvironment"
        private const val REST_REGULATION_ENVIRONMENT_KEY = "regulationEnvironment"

        @JvmStatic
        fun fromJson(json: JSONObject?): AuthenticationInsight? {

            if (json == null) {
                return null
            }

            val regulationEnv = if (json.has(GRAPHQL_REGULATION_ENVIRONMENT_KEY)) {
                Json.optString(json, GRAPHQL_REGULATION_ENVIRONMENT_KEY, "")
            } else {
                Json.optString(json, REST_REGULATION_ENVIRONMENT_KEY, "")
            }.let { environmentKey ->
                environmentKey.lowercase().let {
                    if ("psdtwo" == it) {
                        "psd2"
                    } else {
                        it
                    }
                }
            }

            return AuthenticationInsight(regulationEnv)
        }
    }
}
