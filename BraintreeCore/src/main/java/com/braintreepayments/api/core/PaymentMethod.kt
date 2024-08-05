package com.braintreepayments.api.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * An abstract class to extend when creating a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
abstract class PaymentMethod(
    open var integration: String? = DEFAULT_INTEGRATION,
    open var source: String? = DEFAULT_SOURCE,
    open var sessionId: String? = null,
    open var apiPath: String
) : Parcelable {

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        const val OPERATION_NAME_KEY = "operationName"
        const val OPTIONS_KEY = "options"
        const val VALIDATE_KEY = "validate"

        private const val DEFAULT_SOURCE = "form"
        private const val DEFAULT_INTEGRATION = "custom"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(sessionId)
            .source(source)
            .integration(integration)
            .build()
    }

    @Throws(JSONException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun buildJSON(): JSONObject {
        val base = JSONObject()
        base.put(MetadataBuilder.META_KEY, buildMetadataJSON())
        return base
    }
}
