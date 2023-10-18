package com.braintreepayments.api

import android.text.TextUtils
import androidx.annotation.RestrictTo

/**
 * Generic base class for Braintree authorization
 * @property configUrl The url to fetch configuration for the current Braintree environment.
 * @property bearer The authorization bearer string for authorizing requests.
 * @property rawValue The original Client token or Tokenization Key string, which can be used for serialization
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class Authorization(private val rawValue: String) {

    abstract val configUrl: String?
    abstract val bearer: String?

    /**
     * @return The original Client token or Tokenization Key string, which can be used for serialization
     */
    override fun toString(): String {
        return rawValue
    }

    companion object {
        /**
         * Returns an [Authorization] of the correct type for a given [String]. If an
         * invalid authorization string is provided, an [InvalidAuthorization] will be returned.
         * Requests with an [InvalidAuthorization] will return a [BraintreeException] to the
         * [HttpResponseCallback].
         *
         * @param authorizationString Given string to transform into an [Authorization].
         * @return [Authorization]
         */
        @JvmStatic
        fun fromString(authorizationString: String?): Authorization {
            val authTrimmed = authorizationString?.trim { it <= ' ' }
            if (authTrimmed == null || TextUtils.isEmpty(authorizationString)) {
                val errorMessage = "Authorization provided is invalid: $authTrimmed"
                return InvalidAuthorization(authTrimmed ?: "null", errorMessage)
            }
            return try {
                    if (isTokenizationKey(authTrimmed)) {
                        TokenizationKey(authTrimmed)
                    } else if (isClientToken(authTrimmed)) {
                        ClientToken(authTrimmed)
                    } else {
                        val errorMessage = "Authorization provided is invalid: $authTrimmed"
                        InvalidAuthorization(authTrimmed, errorMessage)
                    }
                } catch (error: InvalidArgumentException) {
                    InvalidAuthorization(authTrimmed, error.message!!)
                }
        }

        private fun isTokenizationKey(tokenizationKey: String): Boolean {
            return tokenizationKey.matches(Regex(TokenizationKey.MATCHER))
        }

        private fun isClientToken(clientToken: String): Boolean {
            return clientToken.matches(Regex(ClientToken.BASE_64_MATCHER))
        }
    }
}
