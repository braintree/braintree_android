package com.braintreepayments.api.visacheckout

import android.os.Parcel
import android.os.Parcelable
import com.braintreepayments.api.card.BinData
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a Visa Checkout card.
 *
 * @see PaymentMethodNonce
 *
 * @property lastTwo Last two digits of the user's underlying card, intended for display purposes.
 * @property cardType Type of this card (e.g. Visa, MasterCard, American Express)
 * @property billingAddress The user's billing address.
 * @property shippingAddress The user's shipping address.
 * @property userData The user's data.
 * @property callId The Call ID from the VisaPaymentSummary.
 * @property binData The BIN data for the card number associated with [VisaCheckoutNonce]
 */
@Parcelize
data class VisaCheckoutNonce internal constructor(
    val lastTwo: String,
    val cardType: String,
    val billingAddress: VisaCheckoutAddress,
    val shippingAddress: VisaCheckoutAddress,
    val userData: VisaCheckoutUserData,
    val callId: String,
    val binData: BinData,
    override val string: String,
    override val isDefault: Boolean
) : PaymentMethodNonce(string, isDefault) {

    companion object {
        private const val API_RESOURCE_KEY = "visaCheckoutCards"

        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        private const val CARD_DETAILS_KEY = "details"
        private const val CARD_TYPE_KEY = "cardType"
        private const val LAST_TWO_KEY = "lastTwo"
        private const val BILLING_ADDRESS_KEY = "billingAddress"
        private const val SHIPPING_ADDRESS_KEY = "shippingAddress"
        private const val USER_DATA_KEY = "userData"
        private const val CALL_ID_KEY = "callId"

        @Throws(JSONException::class)
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): VisaCheckoutNonce {
            val json = if (inputJson.has(API_RESOURCE_KEY)) {
                inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            } else {
                inputJson
            }

            val details = json.getJSONObject(CARD_DETAILS_KEY)
            return VisaCheckoutNonce(
                lastTwo = details.getString(LAST_TWO_KEY),
                cardType = details.getString(CARD_TYPE_KEY),
                billingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(BILLING_ADDRESS_KEY)),
                shippingAddress = VisaCheckoutAddress.fromJson(json.optJSONObject(SHIPPING_ADDRESS_KEY)),
                userData = VisaCheckoutUserData.fromJson(json.optJSONObject(USER_DATA_KEY)),
                callId = Json.optString(json, CALL_ID_KEY, ""),
                binData = BinData.fromJson(json.optJSONObject(BinData.BIN_DATA_KEY)),
                string = json.getString(PAYMENT_METHOD_NONCE_KEY),
                isDefault = json.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)
            )
        }
    }
}