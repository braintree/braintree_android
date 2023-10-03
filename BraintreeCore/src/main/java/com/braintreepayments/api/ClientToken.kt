package com.braintreepayments.api

import android.util.Base64
import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject
import java.lang.NullPointerException
import kotlin.jvm.Throws

// NEXT MAJOR VERSION: remove authorizationFingerprint. Keep bearer

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 * @param clientTokenString A client token from the Braintree Gateway
 * @property authorizationFingerprint The authorizationFingerprint for the current session
 * @property customerId The customer ID in the authorizationFingerprint if it is present
 * @constructor Create a new [ClientToken] instance from a client token
 * @throws InvalidArgumentException when client token is invalid
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("SwallowedException", "TooGenericExceptionCaught")
class ClientToken @Throws(InvalidArgumentException::class) constructor(
    clientTokenString: String
) : Authorization(clientTokenString) {

    override val configUrl: String
    override val bearer: String

    val authorizationFingerprint: String
    val customerId: String?

    init {
        try {
            val clientTokenStringDecoded = String(Base64.decode(clientTokenString, Base64.DEFAULT))
            val jsonObject = JSONObject(clientTokenStringDecoded)
            configUrl = jsonObject.getString(CONFIG_URL_KEY)
            authorizationFingerprint = jsonObject.getString(AUTHORIZATION_FINGERPRINT_KEY)
            bearer = authorizationFingerprint
            customerId = parseCustomerId(authorizationFingerprint)
        } catch (e: NullPointerException) {
            throw InvalidArgumentException("Client token was invalid")
        } catch (e: JSONException) {
            throw InvalidArgumentException("Client token was invalid")
        }
    }

    companion object {
        const val BASE_64_MATCHER =
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)"
        private const val CONFIG_URL_KEY = "configUrl"
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint"

        private fun parseCustomerId(authorizationFingerprint: String?): String? {
            val result = authorizationFingerprint?.let { fingerPrint ->
                val components = fingerPrint.split("&")
                for (component in components) {
                    if (component.contains("customer_id=")) {
                        val customerComponents = component.split("=")
                        if (customerComponents.size > 1) {
                            return customerComponents[1]
                        }
                    }
                }
                return null
            }
            return result
        }
    }
}
