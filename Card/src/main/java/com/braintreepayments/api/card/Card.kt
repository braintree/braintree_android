package com.braintreepayments.api.card

import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.PaymentMethod
import com.braintreepayments.api.core.PaymentMethod.Companion.DEFAULT_SOURCE
import com.braintreepayments.api.core.PaymentMethod.Companion.OPERATION_NAME_KEY
import com.braintreepayments.api.core.PaymentMethod.Companion.OPTIONS_KEY
import com.braintreepayments.api.core.PaymentMethod.Companion.VALIDATE_KEY
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Use to construct a card tokenization request.
 *
 * @property merchantAccountId The merchant account id used to generate the authentication insight.
 * @property isAuthenticationInsightRequested If authentication insight will be requested.
 * @property shouldValidate Flag to denote if the associated {@link Card} will be validated. Defaults to false.
 * <p>
 * Use this flag with caution. Enabling validation may result in adding a card to the Braintree vault.
 * The circumstances that determine if a Card will be vaulted are not documented.
 * @property cardholderName Name on the card.
 * @property number The card number.
 * @property company Company associated with the card.
 * @property countryCode The ISO 3166-1 alpha-3 country code specified in the card's billing address.
 * @property cvv The card verification code (like CVV or CID).
 * If you wish to create a CVV-only payment method nonce to verify a card already stored in your Vault,
 * omit all other properties to only collect CVV.
 * @property expirationMonth The expiration month of the card.
 * @property expirationYear The expiration year of the card.
 * @property extendedAddress The extended address of the card.
 * @property firstName First name on the card.
 * @property lastName Last name on the card.
 * @property locality Locality of the card.
 * @property postalCode Postal code of the card.
 * @property region Region of the card.
 * @property streetAddress Street address of the card.
 * @property sessionId The session id associated with this request. The session is a uuid.
 * This field is automatically set at the point of tokenization, and any previous
 * values ignored.
 * @property source The source associated with the tokenization call for analytics use. Set automatically.
 * @property integration The integration method associated with the tokenization call for analytics use.
 * Defaults to custom and does not need to ever be set.
 */
@Parcelize
data class Card @JvmOverloads constructor(
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
    override var integration: IntegrationType? = IntegrationType.CUSTOM
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

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val apiPath: String
        get() = "credit_cards"

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
