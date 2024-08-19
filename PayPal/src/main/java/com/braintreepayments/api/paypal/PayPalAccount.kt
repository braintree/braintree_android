package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethod
import org.json.JSONException
import org.json.JSONObject

/**
 * Use to construct a PayPal account tokenization request
 *
 * @property clientMetadataId Application clientMetadataId created by
 * [DataCollector.getClientMetadataId]. Used by PayPal wrappers to construct a request to create a
 * PayPal account.
 * @property urlResponseData Response data from callback url. Used by PayPal wrappers to construct a
 * request to create a PayPal account. Response data will be merged into the payment method json on
 * [buildJSON].
 * @property intent Used by PayPal wrappers to construct a request to create a PayPal account.
 * @property merchantAccountId String merchant account id
 * @property paymentType Payment type from original PayPal request. Either "billing-agreement" or
 * "single-payment"
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalAccount(
    val clientMetadataId: String?,
    val urlResponseData: JSONObject,
    val intent: PayPalPaymentIntent?,
    val merchantAccountId: String?,
    val paymentType: String?
) : PaymentMethod() {

    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject? {
        val json = super.buildJSON()

        val paymentMethodNonceJson = JSONObject()
        paymentMethodNonceJson.put(CORRELATION_ID_KEY, clientMetadataId)
        paymentMethodNonceJson.put(INTENT_KEY, intent?.stringValue)

        if ("single-payment".equals(paymentType, ignoreCase = true)) {
            val optionsJson = JSONObject()
            optionsJson.put(VALIDATE_KEY, false)
            paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson)
        }

        val urlResponseDataKeyIterator = urlResponseData.keys()
        while (urlResponseDataKeyIterator.hasNext()) {
            val key = urlResponseDataKeyIterator.next()
            paymentMethodNonceJson.put(key, urlResponseData[key])
        }

        if (merchantAccountId != null) {
            json?.put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
        }
        json?.put(PAYPAL_ACCOUNT_KEY, paymentMethodNonceJson)
        return json
    }

    override val apiPath: String
        get() = "paypal_accounts"

    companion object {
        private const val PAYPAL_ACCOUNT_KEY = "paypalAccount"
        private const val CORRELATION_ID_KEY = "correlationId"
        private const val INTENT_KEY = "intent"
        private const val MERCHANT_ACCOUNT_ID_KEY = "merchant_account_id"
    }
}
