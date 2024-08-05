package com.braintreepayments.api.card

import android.text.TextUtils
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Use to construct a card tokenization request.
 */
@Parcelize
data class Card(
    var merchantAccountId: String? = null,
    var isAuthenticationInsightRequested: Boolean = false,
    var shouldValidate: Boolean = false,
    override var cardholderName: String? = null,
    override var number: String? = null,
    override var company: String? = null,
    override var countryCode: String? = null,
    override var cvv: String? = null,
    override var expirationMonth: String? = null,
    override var expirationYear: String? = null,
    override var extendedAddress: String? = null,
    override var firstName: String? = null,
    override var lastName: String? = null,
    override var locality: String? = null,
    override var postalCode: String? = null,
    override var region: String? = null,
    override var streetAddress: String? = null,
    override var integration: String? = null,
    override var source: String? = null,
    override var sessionId: String? = null
) : BaseCard() {

    companion object {
        private const val GRAPHQL_CLIENT_SDK_METADATA_KEY = "clientSdkMetadata"
        private const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"
        private const val AUTHENTICATION_INSIGHT_REQUESTED_KEY = "authenticationInsight"
        private const val AUTHENTICATION_INSIGHT_INPUT_KEY = "authenticationInsightInput"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(BraintreeException::class, JSONException::class)
    fun buildJSONForGraphQL(): JSONObject {

        val input = JSONObject()
        val variables = JSONObject()

        val optionsJson = JSONObject()
        optionsJson.put(VALIDATE_KEY, shouldValidate)
        input.put(OPTIONS_KEY, optionsJson)
        variables.put(GraphQLConstants.Keys.INPUT, input)

        if (TextUtils.isEmpty(merchantAccountId) && isAuthenticationInsightRequested) {
            throw BraintreeException("A merchant account ID is required when authenticationInsightRequested is true.")
        }

        if (isAuthenticationInsightRequested) {
            variables.put(
                AUTHENTICATION_INSIGHT_INPUT_KEY,
                JSONObject().put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
            )
        }

        val creditCard = JSONObject()
            .put(NUMBER_KEY, number)
            .put(EXPIRATION_MONTH_KEY, expirationMonth)
            .put(EXPIRATION_YEAR_KEY, expirationYear)
            .put(CVV_KEY, cvv)
            .put(CARDHOLDER_NAME_KEY, cardholderName)

        val billingAddress = JSONObject()
            .put(FIRST_NAME_KEY, firstName)
            .put(LAST_NAME_KEY, lastName)
            .put(COMPANY_KEY, company)
            .put(COUNTRY_CODE_KEY, countryCode)
            .put(LOCALITY_KEY, locality)
            .put(POSTAL_CODE_KEY, postalCode)
            .put(REGION_KEY, region)
            .put(STREET_ADDRESS_KEY, streetAddress)
            .put(EXTENDED_ADDRESS_KEY, extendedAddress)

        if (billingAddress.length() > 0) {
            creditCard.put(BILLING_ADDRESS_KEY, billingAddress)
        }

        input.put(CREDIT_CARD_KEY, creditCard)

        return JSONObject().apply {
            put(GRAPHQL_CLIENT_SDK_METADATA_KEY, buildMetadataJSON())
            put(GraphQLConstants.Keys.QUERY, cardTokenizationGraphQLMutation)
            put(OPERATION_NAME_KEY, "TokenizeCreditCard")
            put(GraphQLConstants.Keys.VARIABLES, variables)
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Throws(JSONException::class)
    override fun buildJSON(): JSONObject {
        val json = super.buildJSON()

        val paymentMethodNonceJson = json.getJSONObject(CREDIT_CARD_KEY)
        val optionsJson = JSONObject()
        optionsJson.put(VALIDATE_KEY, shouldValidate)
        paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson)

        if (isAuthenticationInsightRequested) {
            json.put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
            json.put(AUTHENTICATION_INSIGHT_REQUESTED_KEY, isAuthenticationInsightRequested)
        }
        return json
    }

    private val cardTokenizationGraphQLMutation: String
        get() {
            val stringBuilder = StringBuilder()
            stringBuilder.append("mutation TokenizeCreditCard(\$input: TokenizeCreditCardInput!")

            if (isAuthenticationInsightRequested) {
                stringBuilder.append(", \$authenticationInsightInput: AuthenticationInsightInput!")
            }

            stringBuilder.append(
                ") {" +
                        "  tokenizeCreditCard(input: \$input) {" +
                        "    token" +
                        "    creditCard {" +
                        "      bin" +
                        "      brand" +
                        "      expirationMonth" +
                        "      expirationYear" +
                        "      cardholderName" +
                        "      last4" +
                        "      binData {" +
                        "        prepaid" +
                        "        healthcare" +
                        "        debit" +
                        "        durbinRegulated" +
                        "        commercial" +
                        "        payroll" +
                        "        issuingBank" +
                        "        countryOfIssuance" +
                        "        productId" +
                        "      }" +
                        "    }"
            )

            if (isAuthenticationInsightRequested) {
                stringBuilder.append(
                    "" +
                            "    authenticationInsight(input: \$authenticationInsightInput) {" +
                            "      customerAuthenticationRegulationEnvironment" +
                            "    }"
                )
            }

            stringBuilder.append(
                "" +
                        "  }" +
                        "}"
            )

            return stringBuilder.toString()
        }
}
