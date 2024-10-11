package com.braintreepayments.api.venmo

import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.PaymentMethod
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_SOURCE
import org.json.JSONException
import org.json.JSONObject

internal class VenmoAccount(
    private val nonce: String? = null,
    override var sessionId: String? = null,
    override var source: String? = DEFAULT_SOURCE,
    override var integration: IntegrationType? = IntegrationType.CUSTOM
) : PaymentMethod {

    private fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(sessionId)
            .source(source)
            .integration(integration)
            .build()
    }

    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject {
        val paymentMethodNonceJson = JSONObject()
        paymentMethodNonceJson.put(NONCE_KEY, nonce)

        return JSONObject().apply {
            put(MetadataBuilder.META_KEY, buildMetadataJSON())
            put(VENMO_ACCOUNT_KEY, paymentMethodNonceJson)
        }
    }

    override val apiPath = "venmo_accounts"

    companion object {
        private const val VENMO_ACCOUNT_KEY = "venmoAccount"
        private const val NONCE_KEY = "nonce"
    }
}
