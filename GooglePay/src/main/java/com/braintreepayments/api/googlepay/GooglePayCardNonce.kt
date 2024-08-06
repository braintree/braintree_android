package com.braintreepayments.api.googlepay

import com.braintreepayments.api.card.BinData
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a Google Pay card.
 *
 * @see PaymentMethodNonce
 *
 * @property cardType Type of this card (e.g. Visa, MasterCard, American Express)
 * @property bin First six digits of card number.
 * @property lastTwo Last two digits of the user's underlying card, intended for display purposes.
 * @property lastFour Last four digits of the user's underlying card, intended for display purposes.
 * @property email The user's email address associated the Google Pay account.
 * @property cardNetwork The card network. This card network value should not be displayed to the buyer.
 * @property isNetworkTokenized true if the card is network tokenized.
 * @property billingAddress The user's billing address.
 * @property shippingAddress The user's shipping address.
 * @property binData The BIN data for the card number associated with [GooglePayCardNonce]
 */
@Parcelize
data class GooglePayCardNonce internal constructor(
    override val string: String,
    override val isDefault: Boolean,
    val cardType: String,
    val bin: String,
    val lastTwo: String,
    val lastFour: String,
    val email: String,
    val cardNetwork: String,
    var isNetworkTokenized: Boolean,
    val billingAddress: PostalAddress,
    val shippingAddress: PostalAddress,
    val binData: BinData
) : PaymentMethodNonce(
    string = string,
    isDefault = isDefault,
) {

    companion object {
        const val API_RESOURCE_KEY: String = "androidPayCards"

        private const val CARD_DETAILS_KEY = "details"
        private const val CARD_TYPE_KEY = "cardType"
        private const val BIN_KEY = "bin"
        private const val LAST_TWO_KEY = "lastTwo"
        private const val LAST_FOUR_KEY = "lastFour"
        private const val IS_NETWORK_TOKENIZED_KEY = "isNetworkTokenized"
        private const val CARD_NETWORK_KEY = "cardNetwork"

        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        @Throws(JSONException::class)
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): PaymentMethodNonce {
            val tokenPayload = JSONObject(
                inputJson
                    .getJSONObject("paymentMethodData")
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            return if (tokenPayload.has(API_RESOURCE_KEY)) {
                fromGooglePayJSON(inputJson)
            } else if (tokenPayload.has(PayPalAccountNonce.API_RESOURCE_KEY)) {
                PayPalAccountNonce.fromJSON(inputJson)
            } else {
                throw JSONException("Could not parse JSON for a payment method nonce")
            }
        }

        @Throws(JSONException::class)
        private fun fromGooglePayJSON(inputJson: JSONObject): GooglePayCardNonce {
            val tokenPayload = JSONObject(
                inputJson
                    .getJSONObject("paymentMethodData")
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            val androidPayCardObject = tokenPayload.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            val nonce = androidPayCardObject.getString(PAYMENT_METHOD_NONCE_KEY)
            val isDefault = androidPayCardObject.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)

            val details = androidPayCardObject.getJSONObject(CARD_DETAILS_KEY)
            val info = inputJson
                .getJSONObject("paymentMethodData")
                .getJSONObject("info")

            val cardNetwork = info.getString(CARD_NETWORK_KEY)

            var billingAddressJson = JSONObject()
            if (info.has("billingAddress")) {
                billingAddressJson = info.getJSONObject("billingAddress")
            }

            var shippingAddressJson = JSONObject()
            if (inputJson.has("shippingAddress")) {
                shippingAddressJson = inputJson.getJSONObject("shippingAddress")
            }

            val email = Json.optString(inputJson, "email", "")
            val billingAddress = postalAddressFromJson(billingAddressJson)
            val shippingAddress = postalAddressFromJson(shippingAddressJson)

            val binData = BinData.fromJson(inputJson.optJSONObject(BinData.BIN_DATA_KEY))
            val bin = details.getString(BIN_KEY)
            val lastTwo = details.getString(LAST_TWO_KEY)
            val lastFour = details.getString(LAST_FOUR_KEY)
            val cardType = details.getString(CARD_TYPE_KEY)
            val isNetworkTokenized = details.optBoolean(IS_NETWORK_TOKENIZED_KEY, false)

            return GooglePayCardNonce(
                string = nonce,
                isDefault = isDefault,
                cardType = cardType,
                bin = bin,
                lastTwo = lastTwo,
                lastFour = lastFour,
                email = email,
                cardNetwork = cardNetwork,
                isNetworkTokenized = isNetworkTokenized,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress,
                binData = binData,
            )
        }

        private fun postalAddressFromJson(json: JSONObject?): PostalAddress {
            val address = PostalAddress()

            address.recipientName = Json.optString(json, "name", "")
            address.phoneNumber = Json.optString(json, "phoneNumber", "")
            address.streetAddress = Json.optString(json, "address1", "")
            address.extendedAddress = formatExtendedAddress(json)
            address.locality = Json.optString(json, "locality", "")
            address.region = Json.optString(json, "administrativeArea", "")
            address.countryCodeAlpha2 = Json.optString(json, "countryCode", "")
            address.postalCode = Json.optString(json, "postalCode", "")
            address.sortingCode = Json.optString(json, "sortingCode", "")

            return address
        }

        private fun formatExtendedAddress(address: JSONObject?): String {
            val extendedAddress = """
                ${Json.optString(address, "address2", "")}
                ${Json.optString(address, "address3", "")}
                ${Json.optString(address, "address4", "")}
                ${Json.optString(address, "address5", "")}
                """.trimIndent()

            return extendedAddress.trim { it <= ' ' }
        }
    }
}
