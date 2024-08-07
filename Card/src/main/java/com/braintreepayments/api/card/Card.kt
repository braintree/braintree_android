package com.braintreepayments.api.card

import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.PaymentMethod
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_INTEGRATION
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_SOURCE
import com.braintreepayments.api.core.PaymentMethod.Companion.OPERATION_NAME_KEY
import com.braintreepayments.api.core.PaymentMethod.Companion.OPTIONS_KEY
import com.braintreepayments.api.core.PaymentMethod.Companion.VALIDATE_KEY
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
    var cardholderName: String? = null,
    var number: String? = null,
    var company: String? = null,
    var countryCode: String? = null,
    var cvv: String? = null,
    var expirationMonth: String? = null,
    var expirationYear: String? = null,
    var extendedAddress: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var locality: String? = null,
    var postalCode: String? = null,
    var region: String? = null,
    var streetAddress: String? = null,
    override var sessionId: String? = null,
    override var source: String? = DEFAULT_SOURCE,
    override var integration: String? = DEFAULT_INTEGRATION,
    override val apiPath: String = "credit_cards"
) : PaymentMethod, Parcelable {

    companion object {
        private const val BILLING_ADDRESS_KEY: String = "billingAddress"
        private const val CARDHOLDER_NAME_KEY: String = "cardholderName"
        private const val COMPANY_KEY: String = "company"
        private const val COUNTRY_CODE_ALPHA3_KEY: String = "countryCodeAlpha3"
        private const val COUNTRY_CODE_KEY: String = "countryCode"
        private const val CREDIT_CARD_KEY: String = "creditCard"
        private const val CVV_KEY: String = "cvv"
        private const val EXPIRATION_MONTH_KEY: String = "expirationMonth"
        private const val EXPIRATION_YEAR_KEY: String = "expirationYear"
        private const val EXTENDED_ADDRESS_KEY: String = "extendedAddress"
        private const val FIRST_NAME_KEY: String = "firstName"
        private const val LAST_NAME_KEY: String = "lastName"
        private const val LOCALITY_KEY: String = "locality"
        private const val NUMBER_KEY: String = "number"
        private const val POSTAL_CODE_KEY: String = "postalCode"
        private const val REGION_KEY: String = "region"
        private const val STREET_ADDRESS_KEY: String = "streetAddress"
        private const val GRAPHQL_CLIENT_SDK_METADATA_KEY = "clientSdkMetadata"
        private const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"
        private const val AUTHENTICATION_INSIGHT_REQUESTED_KEY = "authenticationInsight"
        private const val AUTHENTICATION_INSIGHT_INPUT_KEY = "authenticationInsightInput"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private fun buildMetadataJSON(): JSONObject {
        return MetadataBuilder()
            .sessionId(sessionId)
            .source(source)
            .integration(integration)
            .build()
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
        return JSONObject().apply {
            put(MetadataBuilder.META_KEY, buildMetadataJSON())

            val billingAddressJson = JSONObject().apply {
                put(FIRST_NAME_KEY, firstName)
                put(LAST_NAME_KEY, lastName)
                put(COMPANY_KEY, company)
                put(LOCALITY_KEY, locality)
                put(POSTAL_CODE_KEY, postalCode)
                put(REGION_KEY, region)
                put(STREET_ADDRESS_KEY, streetAddress)
                put(EXTENDED_ADDRESS_KEY, extendedAddress)
                if (countryCode != null) {
                    put(COUNTRY_CODE_ALPHA3_KEY, countryCode)
                }
            }

            val paymentMethodNonceJson = JSONObject().apply {
                put(NUMBER_KEY, number)
                put(CVV_KEY, cvv)
                put(EXPIRATION_MONTH_KEY, expirationMonth)
                put(EXPIRATION_YEAR_KEY, expirationYear)
                put(CARDHOLDER_NAME_KEY, cardholderName)
                put(OPTIONS_KEY, JSONObject().apply {
                    put(VALIDATE_KEY, shouldValidate)
                })
                if (billingAddressJson.length() > 0) {
                    put(BILLING_ADDRESS_KEY, billingAddressJson)
                }
            }

            put(CREDIT_CARD_KEY, paymentMethodNonceJson)

            if (isAuthenticationInsightRequested) {
                put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
                put(AUTHENTICATION_INSIGHT_REQUESTED_KEY, isAuthenticationInsightRequested)
            }
        }
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
