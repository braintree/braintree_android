package com.braintreepayments.api.venmo

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethod
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_INTEGRATION
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_SOURCE
import org.json.JSONException
import org.json.JSONObject

internal class VenmoAccount(
    var nonce: String? = null,
    override var sessionId: String? = null,
    override var source: String? = DEFAULT_SOURCE,
    override var integration: String? = DEFAULT_INTEGRATION
) : PaymentMethod {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject {

        val paymentMethodNonceJson = JSONObject()
        paymentMethodNonceJson.put(NONCE_KEY, nonce)

        return super.buildJSON().apply {
            put(VENMO_ACCOUNT_KEY, paymentMethodNonceJson)
        }
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val apiPath: String
        /**
         * @hide
         */
        get() = "venmo_accounts"

    companion object {
        private const val VENMO_ACCOUNT_KEY = "venmoAccount"
        private const val NONCE_KEY = "nonce"
    }
}
