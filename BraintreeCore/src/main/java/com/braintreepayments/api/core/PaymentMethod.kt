package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

interface PaymentMethod {

    var sessionId: String?
    var source: String?
    var integration: String?
    val apiPath: String

    companion object {
        const val OPERATION_NAME_KEY = "operationName"
        const val OPTIONS_KEY = "options"
        const val VALIDATE_KEY = "validate"
        const val DEFAULT_SOURCE = "form"
        const val DEFAULT_INTEGRATION = "custom"
    }

    @Throws(JSONException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildJSON(): JSONObject
}