package com.braintreepayments.api.visacheckout

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethod
import com.visa.checkout.VisaPaymentSummary
import org.json.JSONException
import org.json.JSONObject

/**
 * Use to construct a Visa Checkout tokenization request.
 */
internal class VisaCheckoutAccount(
    private val visaPaymentSummary: VisaPaymentSummary
) : PaymentMethod() {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject? {
        val json = super.buildJSON()
        val paymentMethodNonceJson = JSONObject().apply {
            put(CALL_ID, visaPaymentSummary.callId)
            put(ENCRYPTED_KEY, visaPaymentSummary.encKey)
            put(ENCRYPTED_PAYMENT_DATA, visaPaymentSummary.encPaymentData)
        }
        json?.put(VISA_CHECKOUT_KEY, paymentMethodNonceJson)
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
