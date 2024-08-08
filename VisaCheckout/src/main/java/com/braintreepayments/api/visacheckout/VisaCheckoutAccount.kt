package com.braintreepayments.api.visacheckout

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.PaymentMethod
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_SOURCE
import com.visa.checkout.VisaPaymentSummary
import org.json.JSONException
import org.json.JSONObject

/**
 * Use to construct a Visa Checkout tokenization request.
 */
internal class VisaCheckoutAccount(
    private val visaPaymentSummary: VisaPaymentSummary,
    override var sessionId: String? = null,
    override var source: String? = DEFAULT_SOURCE,
    override var integration: IntegrationType? = IntegrationType.CUSTOM
) : PaymentMethod {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(sessionId)
            .source(source)
            .integration(integration)
            .build()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject {
        val json = JSONObject()
            .put(MetadataBuilder.META_KEY, buildMetadataJSON())
        val paymentMethodNonceJson = JSONObject().apply {
            put(CALL_ID, visaPaymentSummary.callId)
            put(ENCRYPTED_KEY, visaPaymentSummary.encKey)
            put(ENCRYPTED_PAYMENT_DATA, visaPaymentSummary.encPaymentData)
        }
        json.put(VISA_CHECKOUT_KEY, paymentMethodNonceJson)
        return json
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val apiPath = "visa_checkout_cards"

    companion object {
        private const val CALL_ID = "callId"
        private const val ENCRYPTED_KEY = "encryptedKey"
        private const val ENCRYPTED_PAYMENT_DATA = "encryptedPaymentData"
        private const val VISA_CHECKOUT_KEY = "visaCheckoutCard"
    }
}
