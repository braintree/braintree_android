package com.braintreepayments.api.localpayment

import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.PostalAddressParser.fromJson
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a local payment.
 *
 * @see PaymentMethodNonce
 *
 * @property clientMetadataId The ClientMetadataId associated with this transaction.
 * @property billingAddress The billing address of the user if requested with additional scopes.
 * @property shippingAddress The shipping address of the user provided by checkout flows.
 * @property givenName The first name associated with the local payment.
 * @property surname The last name associated with the local payment.
 * @property phone The phone number associated with the local payment.
 * @property email The email address associated with this local payment
 * @property payerId The Payer ID provided in local payment flows.
 */
@Parcelize
data class LocalPaymentNonce internal constructor(
    override val string: String,
    override val isDefault: Boolean,
    val clientMetadataId: String?,
    val billingAddress: PostalAddress,
    val shippingAddress: PostalAddress,
    val givenName: String,
    val surname: String,
    val phone: String,
    val email: String?,
    val payerId: String,
) : PaymentMethodNonce(
    string = string,
    isDefault = isDefault,
) {

    companion object {
        private const val API_RESOURCE_KEY = "paypalAccounts"

        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        private const val DETAILS_KEY = "details"
        private const val EMAIL_KEY = "email"
        private const val PAYER_INFO_KEY = "payerInfo"
        private const val ACCOUNT_ADDRESS_KEY = "accountAddress"
        private const val SHIPPING_ADDRESS_KEY = "shippingAddress"
        private const val BILLING_ADDRESS_KEY = "billingAddress"
        private const val FIRST_NAME_KEY = "firstName"
        private const val LAST_NAME_KEY = "lastName"
        private const val PHONE_KEY = "phone"
        private const val PAYER_ID_KEY = "payerId"
        private const val CLIENT_METADATA_ID_KEY = "correlationId"

        @Throws(JSONException::class)
        @Suppress("ThrowsCount")
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): LocalPaymentNonce {
            val json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            val details = json.getJSONObject(DETAILS_KEY)
            val nonce = json.getString(PAYMENT_METHOD_NONCE_KEY)
            val isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)
            var email = Json.optString(details, EMAIL_KEY, null)
            val clientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null)

            var billingAddress: PostalAddress
            var shippingAddress: PostalAddress
            var givenName: String? = null
            var surname: String? = null
            var phone: String? = null
            var payerId: String? = null
            try {
                val payerInfo = details.getJSONObject(PAYER_INFO_KEY)
                val billingAddressJson = if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                    payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY)
                } else {
                    payerInfo.optJSONObject(BILLING_ADDRESS_KEY)
                }

                val shippingAddressJson = payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY)

                billingAddress = fromJson(billingAddressJson)
                shippingAddress = fromJson(shippingAddressJson)

                givenName = Json.optString(payerInfo, FIRST_NAME_KEY, "")
                surname = Json.optString(payerInfo, LAST_NAME_KEY, "")
                phone = Json.optString(payerInfo, PHONE_KEY, "")
                payerId = Json.optString(payerInfo, PAYER_ID_KEY, "")

                if (email == null) {
                    email = Json.optString(payerInfo, EMAIL_KEY, null)
                }
            } catch (e: JSONException) {
                billingAddress = PostalAddress()
                shippingAddress = PostalAddress()
            }

            return LocalPaymentNonce(
                string = nonce,
                isDefault = isDefault,
                clientMetadataId = clientMetadataId,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
                givenName = givenName ?: throw JSONException("givenName is null"),
                surname = surname ?: throw JSONException("surname is null"),
                phone = phone ?: throw JSONException("phone is null"),
                email = email,
                payerId = payerId ?: throw JSONException("payerId is null"),
            )
        }
    }
}
