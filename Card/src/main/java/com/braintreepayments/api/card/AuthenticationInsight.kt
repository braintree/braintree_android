package com.braintreepayments.api.card

import android.os.Parcelable
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

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
