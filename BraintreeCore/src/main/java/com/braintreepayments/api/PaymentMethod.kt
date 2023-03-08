package com.braintreepayments.api

import android.os.Parcel
import org.json.JSONException
import org.json.JSONObject

/**
 * An abstract class to extend when creating a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
abstract class PaymentMethod {
    var integration: String? = DEFAULT_INTEGRATION
    var source: String? = DEFAULT_SOURCE
    var sessionId: String? = null

    internal constructor()

    fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(sessionId)
            .source(source)
            .integration(integration)
            .build()
    }

    @Throws(JSONException::class)
    open fun buildJSON(): JSONObject? {
        val base = JSONObject()
        base.put(MetadataBuilder.META_KEY, buildMetadataJSON())
        return base
    }

    constructor(parcel: Parcel) {
        integration = parcel.readString()
        source = parcel.readString()
        sessionId = parcel.readString()
    }

    open fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(integration)
        dest.writeString(source)
        dest.writeString(sessionId)
    }

    abstract val apiPath: String?
        get

    companion object {
        const val OPERATION_NAME_KEY = "operationName"
        const val OPTIONS_KEY = "options"
        const val VALIDATE_KEY = "validate"
        private const val DEFAULT_SOURCE = "form"
        private const val DEFAULT_INTEGRATION = "custom"
    }
}
