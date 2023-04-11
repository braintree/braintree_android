package com.braintreepayments.api

import android.os.Parcel
import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * An abstract class to extend when creating a payment method. Contains logic and
 * implementations shared by all payment methods.
 */
abstract class PaymentMethod {

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

    private var _sessionId: String? = null
    private var _source: String? = DEFAULT_SOURCE
    private var _integration: String? = DEFAULT_INTEGRATION

    abstract val apiPath: String?

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor()

    /**
     * Sets the integration method associated with the tokenization call for analytics use.
     * Defaults to custom and does not need to ever be set.
     *
     * @param integration the current integration style.
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setIntegration(integration: String) {
        _integration = integration
    }

    /**
     * Sets the source associated with the tokenization call for analytics use. Set automatically.
     *
     * @param source the source of the payment method.
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setSource(source: String) {
        _source = source
    }

    /**
     * @param sessionId sets the session id associated with this request. The session is a uuid.
     * This field is automatically set at the point of tokenization, and any previous
     * values ignored.
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setSessionId(sessionId: String?) {
        // TODO: require session id in constructor to eliminate the possibility of a null value
        _sessionId = sessionId
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(_sessionId)
            .source(_source)
            .integration(_integration)
            .build()
    }

    @Throws(JSONException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun buildJSON(): JSONObject? {
        val base = JSONObject()
        base.put(MetadataBuilder.META_KEY, buildMetadataJSON())
        return base
    }

    protected constructor(parcel: Parcel) {
        _integration = parcel.readString()
        _source = parcel.readString()
        _sessionId = parcel.readString()
    }

    open fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(_integration)
        dest.writeString(_source)
        dest.writeString(_sessionId)
    }
}
