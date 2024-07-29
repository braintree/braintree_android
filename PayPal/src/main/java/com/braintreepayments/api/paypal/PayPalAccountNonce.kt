package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.core.PostalAddressParser.fromJson
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a PayPal account.
 *
 * @see PaymentMethodNonce
 *
 * @property clientMetadataId The ClientMetadataId associated with this transaction.
 * @property billingAddress The billing address of the user if requested with additional scopes.
 * @property shippingAddress The shipping address of the user provided by checkout flows.
 * @property firstName The first name associated with the PayPal account.
 * @property lastName The last name associated with the PayPal account.
 * @property phone The phone number associated with the PayPal account.
 * @property email The email address associated with this PayPal account
 * @property payerId The Payer ID provided in checkout flows.
 * @property creditFinancing The credit financing details. This property will only be present when
 * the customer pays with PayPal Credit.
 * @property authenticateUrl The URL used to authenticate the customer during two-factor
 * authentication flows. This property will only be present if two-factor authentication is
 * required.
 */
@Parcelize
class PayPalAccountNonce internal constructor(
    override val string: String,
    override val isDefault: Boolean,
    val clientMetadataId: String?,
    val billingAddress: PostalAddress,
    val shippingAddress: PostalAddress,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String?,
    val payerId: String,
    val creditFinancing: PayPalCreditFinancing?,
    val authenticateUrl: String?,
) : PaymentMethodNonce(
    string = string,
    isDefault = isDefault,
) {

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val API_RESOURCE_KEY: String = "paypalAccounts"
        private const val PAYMENT_METHOD_DATA_KEY = "paymentMethodData"
        private const val TOKENIZATION_DATA_KEY = "tokenizationData"
        private const val TOKEN_KEY = "token"

        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        private const val CREDIT_FINANCING_KEY = "creditFinancingOffered"
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        @Suppress("LongMethod")
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): PayPalAccountNonce {
            var getShippingAddressFromTopLevel = false

            val json: JSONObject
            if (inputJson.has(API_RESOURCE_KEY)) {
                json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            } else if (inputJson.has(PAYMENT_METHOD_DATA_KEY)) {
                getShippingAddressFromTopLevel = true
                val tokenObj = JSONObject(
                    inputJson
                        .getJSONObject(PAYMENT_METHOD_DATA_KEY)
                        .getJSONObject(TOKENIZATION_DATA_KEY)
                        .getString(TOKEN_KEY)
                )
                json = tokenObj.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            } else {
                json = inputJson
            }

            val nonce = json.getString(PAYMENT_METHOD_NONCE_KEY)
            val isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)

            val authenticateUrl = Json.optString(json, "authenticateUrl", null)

            val details = json.getJSONObject(DETAILS_KEY)
            var email = Json.optString(details, EMAIL_KEY, null)
            val clientMetadataId = Json.optString(details, CLIENT_METADATA_ID_KEY, null)

            var payPalCreditFinancing: PayPalCreditFinancing? = null
            var shippingAddress: PostalAddress
            var billingAddress: PostalAddress
            var firstName = ""
            var lastName = ""
            var phone = ""
            var payerId = ""
            try {
                if (details.has(CREDIT_FINANCING_KEY)) {
                    val creditFinancing = details.getJSONObject(CREDIT_FINANCING_KEY)
                    payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancing)
                }

                val payerInfo = details.getJSONObject(PAYER_INFO_KEY)

                var billingAddressJson = payerInfo.optJSONObject(BILLING_ADDRESS_KEY)
                if (payerInfo.has(ACCOUNT_ADDRESS_KEY)) {
                    billingAddressJson = payerInfo.optJSONObject(ACCOUNT_ADDRESS_KEY)
                }

                shippingAddress = fromJson(payerInfo.optJSONObject(SHIPPING_ADDRESS_KEY))
                billingAddress = fromJson(billingAddressJson)
                firstName = Json.optString(payerInfo, FIRST_NAME_KEY, "")
                lastName = Json.optString(payerInfo, LAST_NAME_KEY, "")
                phone = Json.optString(payerInfo, PHONE_KEY, "")
                payerId = Json.optString(payerInfo, PAYER_ID_KEY, "")

                if (email == null) {
                    email = Json.optString(payerInfo, EMAIL_KEY, null)
                }
            } catch (e: JSONException) {
                billingAddress = PostalAddress()
                shippingAddress = PostalAddress()
            }

            // shipping address should be overriden when 'PAYMENT_METHOD_DATA_KEY' is present at the top-level;
            // this occurs when parsing a GooglePay PayPal Account Nonce
            if (getShippingAddressFromTopLevel) {
                val shippingAddressJson = json.optJSONObject(SHIPPING_ADDRESS_KEY)
                if (shippingAddressJson != null) {
                    shippingAddress = fromJson(shippingAddressJson)
                }
            }

            return PayPalAccountNonce(
                string = nonce,
                isDefault = isDefault,
                clientMetadataId = clientMetadataId,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                email = email,
                payerId = payerId,
                creditFinancing = payPalCreditFinancing,
                authenticateUrl = authenticateUrl
            )
        }
    }
}
