package com.braintreepayments.api.card

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.PaymentMethodNonce
import com.braintreepayments.api.sharedutils.Json
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * [PaymentMethodNonce] representing a credit or debit card.
 *
 * @property string The nonce generated for this payment method by the Braintree gateway. The nonce will
 * represent this PaymentMethod for the purposes of creating transactions and other monetary
 * actions.
 * @property isDefault `true` if this payment method is the default for the current customer, `false` otherwise
 * @property cardType Type of this card (e.g. Visa, MasterCard, American Express)
 * @property lastTwo Last two digits of the card, intended for display purposes.
 * @property lastFour Last four digits of the card.
 * @property bin BIN of the card.
 * @property binData The BIN data for the card number associated with [CardNonce]
 * @property authenticationInsight Details about the regulatory environment and applicable customer
 * authentication regulation for a potential transaction. You may use this to make an informed
 * decision whether to perform 3D Secure authentication.
 * @property expirationMonth The expiration month of the card.
 * @property expirationYear The expiration year of the card.
 * @property cardholderName The name of the cardholder.
 */
@Parcelize
open class CardNonce
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    override val string: String,
    override val isDefault: Boolean,
    open val cardType: String,
    open val lastTwo: String,
    open val lastFour: String,
    open val bin: String,
    open val binData: BinData,
    open val authenticationInsight: AuthenticationInsight?,
    open val expirationMonth: String,
    open val expirationYear: String,
    open val cardholderName: String,
) : PaymentMethodNonce(string, isDefault) {

    companion object {

        /**
         * Parse card nonce from plain JSON object.
         * @param inputJson plain JSON object
         * @return [CardNonce]
         * @throws JSONException if nonce could not be parsed successfully
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Throws(JSONException::class)
        @JvmStatic
        fun fromJSON(inputJson: JSONObject): CardNonce {
            return if (isGraphQLTokenizationResponse(inputJson)) {
                fromGraphQLJSON(inputJson)
            } else if (isRESTfulTokenizationResponse(inputJson)) {
                fromRESTJSON(inputJson)
            } else {
                fromPlainJSONObject(inputJson)
            }
        }

        private fun isGraphQLTokenizationResponse(inputJSON: JSONObject): Boolean {
            return inputJSON.has(DATA_KEY)
        }

        private fun isRESTfulTokenizationResponse(inputJSON: JSONObject): Boolean {
            return inputJSON.has(API_RESOURCE_KEY)
        }

        /**
         * Parse card nonce from RESTful Tokenization response.
         *
         * @param inputJson plain JSON object
         * @return [CardNonce]
         * @throws JSONException if nonce could not be parsed successfully
         */
        @Throws(JSONException::class)
        private fun fromRESTJSON(inputJson: JSONObject): CardNonce {
            val json = inputJson.getJSONArray(API_RESOURCE_KEY).getJSONObject(0)
            return fromPlainJSONObject(json)
        }

        /**
         * Parse card nonce from RESTful Tokenization response.
         *
         * @param inputJson plain JSON object
         * @return [CardNonce]
         * @throws JSONException if nonce could not be parsed successfully
         */
        @Throws(JSONException::class)
        private fun fromPlainJSONObject(inputJson: JSONObject): CardNonce {
            val nonce = inputJson.getString(PAYMENT_METHOD_NONCE_KEY)
            val isDefault = inputJson.optBoolean(PAYMENT_METHOD_DEFAULT_KEY, false)

            val details = inputJson.getJSONObject(CARD_DETAILS_KEY)
            val lastTwo = details.getString(LAST_TWO_KEY)
            val lastFour = details.getString(LAST_FOUR_KEY)
            val cardType = details.getString(CARD_TYPE_KEY)
            val bin = Json.optString(details, BIN_KEY, "")
            val binData = BinData.fromJson(inputJson.optJSONObject(BinData.BIN_DATA_KEY))
            val authenticationInsight = AuthenticationInsight.fromJson(
                inputJson.optJSONObject(AUTHENTICATION_INSIGHT_KEY)
            )
            val expirationMonth = Json.optString(details, EXPIRATION_MONTH_KEY, "")
            val expirationYear = Json.optString(details, EXPIRATION_YEAR_KEY, "")
            val cardholderName = Json.optString(details, CARDHOLDER_NAME_KEY, "")

            return CardNonce(
                string = nonce,
                isDefault = isDefault,
                cardType = cardType,
                lastTwo = lastTwo,
                lastFour = lastFour,
                bin = bin,
                binData = binData,
                authenticationInsight = authenticationInsight,
                expirationMonth = expirationMonth,
                expirationYear = expirationYear,
                cardholderName = cardholderName
            )
        }

        /**
         * Parse card nonce from GraphQL Tokenization response.
         *
         * @param inputJson plain JSON object
         * @return [CardNonce]
         * @throws JSONException if nonce could not be parsed successfully
         */
        @Throws(JSONException::class)
        private fun fromGraphQLJSON(inputJson: JSONObject): CardNonce {
            val data = inputJson.getJSONObject(DATA_KEY)

            if (data.has(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)) {
                val payload = data.getJSONObject(GRAPHQL_TOKENIZE_CREDIT_CARD_KEY)

                val creditCard = payload.getJSONObject(GRAPHQL_CREDIT_CARD_KEY)
                val lastFour = Json.optString(creditCard, GRAPHQL_LAST_FOUR_KEY, "")
                val lastTwo =
                    if (lastFour.length < LAST_FOUR) "" else lastFour.substring(LAST_TWO_INDEX)
                val cardType = Json.optString(creditCard, GRAPHQL_BRAND_KEY, "Unknown")
                val bin = Json.optString(creditCard, "bin", "")
                val binData = BinData.fromJson(creditCard.optJSONObject(BinData.BIN_DATA_KEY))
                val nonce = payload.getString(TOKEN_KEY)
                val authenticationInsight = AuthenticationInsight.fromJson(
                    payload.optJSONObject(AUTHENTICATION_INSIGHT_KEY)
                )
                val expirationMonth = Json.optString(creditCard, EXPIRATION_MONTH_KEY, "")
                val expirationYear = Json.optString(creditCard, EXPIRATION_YEAR_KEY, "")
                val cardholderName = Json.optString(creditCard, CARDHOLDER_NAME_KEY, "")

                return CardNonce(
                    string = nonce,
                    isDefault = false,
                    cardType = cardType,
                    lastTwo = lastTwo,
                    lastFour = lastFour,
                    bin = bin,
                    binData = binData,
                    authenticationInsight = authenticationInsight,
                    expirationMonth = expirationMonth,
                    expirationYear = expirationYear,
                    cardholderName = cardholderName,
                )
            } else {
                throw JSONException("Failed to parse GraphQL response JSON")
            }
        }

        const val API_RESOURCE_KEY: String = "creditCards"
        const val DATA_KEY: String = "data"

        private const val PAYMENT_METHOD_NONCE_KEY = "nonce"
        private const val PAYMENT_METHOD_DEFAULT_KEY = "default"

        private const val TOKEN_KEY = "token"
        private const val GRAPHQL_TOKENIZE_CREDIT_CARD_KEY = "tokenizeCreditCard"
        private const val GRAPHQL_CREDIT_CARD_KEY = "creditCard"
        private const val GRAPHQL_BRAND_KEY = "brand"
        private const val GRAPHQL_LAST_FOUR_KEY = "last4"
        private const val CARD_DETAILS_KEY = "details"
        private const val CARD_TYPE_KEY = "cardType"
        private const val LAST_TWO_KEY = "lastTwo"
        private const val LAST_FOUR_KEY = "lastFour"
        private const val BIN_KEY = "bin"
        private const val AUTHENTICATION_INSIGHT_KEY = "authenticationInsight"
        private const val EXPIRATION_MONTH_KEY = "expirationMonth"
        private const val EXPIRATION_YEAR_KEY = "expirationYear"
        private const val CARDHOLDER_NAME_KEY = "cardholderName"
        private const val LAST_FOUR = 4
        private const val LAST_TWO_INDEX = 2
    }
}
