package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethod] represents the common interface of all payment method.
 *
 * @property sessionId The session id associated with this request. The session is a uuid.
 * This field is automatically set at the point of tokenization, and any previous
 * values ignored.
 * @property source The source associated with the tokenization call for analytics use. Set automatically.
 * @property integration The integration method associated with the tokenization call for analytics use.
 * Defaults to custom and does not need to ever be set.
 * @property apiPath The Api Path of the card
 */
interface PaymentMethod {

    var sessionId: String?
    var source: String?
    var integration: IntegrationType?
    val apiPath: String

    companion object {
        const val OPERATION_NAME_KEY = "operationName"
        const val OPTIONS_KEY = "options"
        const val VALIDATE_KEY = "validate"
        const val DEFAULT_SOURCE = "form"
    }

    @Throws(JSONException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildJSON(): JSONObject
}
