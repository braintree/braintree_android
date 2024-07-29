package com.braintreepayments.api.venmo

import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.PostalAddressParser.fromJson
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a [VenmoAccountNonce]
 *
 * @see PaymentMethodNonce
 *
 * @property email the Venmo user's email
 * @property externalId the Venmo user's external ID
 * @property firstName the Venmo user's first name
 * @property lastName the Venmo user's last name
 * @property phoneNumber the Venmo user's phone number
 * @property username the Venmo username
 * @property billingAddress The Venmo user's billing address.
 * @property shippingAddress The Venmo user's shipping address.
 */
@Parcelize
data class VenmoAccountNonce internal constructor(
    override val string: String,
    override val isDefault: Boolean,
    val email: String?,
    val externalId: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val username: String,
    val billingAddress: PostalAddress?,
    val shippingAddress: PostalAddress?,
) : PaymentMethodNonce(
    string = string,
    isDefault = isDefault,
) {

    companion object {
        private const val API_RESOURCE_KEY = "venmoAccounts"
        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        private const val VENMO_DETAILS_KEY = "details"
        private const val VENMO_USERNAME_KEY = "username"

        private const val VENMO_PAYMENT_METHOD_ID_KEY = "paymentMethodId"
        private const val VENMO_PAYER_INFO_KEY = "payerInfo"
        private const val VENMO_EMAIL_KEY = "email"
        private const val VENMO_EXTERNAL_ID_KEY = "externalId"
        private const val VENMO_FIRST_NAME_KEY = "firstName"
        private const val VENMO_LAST_NAME_KEY = "lastName"
        private const val VENMO_PHONE_NUMBER_KEY = "phoneNumber"
        private const val VENMO_PAYMENT_METHOD_USERNAME_KEY = "userName"
        private const val VENMO_BILLING_ADDRESS_KEY = "billingAddress"
        private const val VENMO_SHIPPING_ADDRESS_KEY = "shippingAddress"

        @Throws(JSONException::class)
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): VenmoAccountNonce {
            val json = if (inputJson.has(API_RESOURCE_KEY)) {
                inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            } else {
                inputJson
            }

            val nonce: String
            val isDefault: Boolean
            val username: String

            if (json.has(VENMO_PAYMENT_METHOD_ID_KEY)) {
                isDefault = false
                nonce = json.getString(VENMO_PAYMENT_METHOD_ID_KEY)
                username = json.getString(VENMO_PAYMENT_METHOD_USERNAME_KEY)
            } else {
                nonce = json.getString(PAYMENT_METHOD_NONCE_KEY)
                isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)

                val details = json.getJSONObject(VENMO_DETAILS_KEY)
                username = details.getString(VENMO_USERNAME_KEY)
            }

            val payerInfo = json.optJSONObject(VENMO_PAYER_INFO_KEY)
            var email: String? = null
            var externalId: String? = null
            var firstName: String? = null
            var lastName: String? = null
            var phoneNumber: String? = null
            var billingAddress: PostalAddress? = null
            var shippingAddress: PostalAddress? = null
            if (payerInfo != null) {
                email = payerInfo.optString(VENMO_EMAIL_KEY)
                externalId = payerInfo.optString(VENMO_EXTERNAL_ID_KEY)
                firstName = payerInfo.optString(VENMO_FIRST_NAME_KEY)
                lastName = payerInfo.optString(VENMO_LAST_NAME_KEY)
                phoneNumber = payerInfo.optString(VENMO_PHONE_NUMBER_KEY)
                billingAddress = fromJson(payerInfo.optJSONObject(VENMO_BILLING_ADDRESS_KEY))
                shippingAddress = fromJson(payerInfo.optJSONObject(VENMO_SHIPPING_ADDRESS_KEY))
            }

            return VenmoAccountNonce(
                string = nonce,
                isDefault = isDefault,
                username = username,
                email = email,
                externalId = externalId,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress
            )
        }
    }
}
