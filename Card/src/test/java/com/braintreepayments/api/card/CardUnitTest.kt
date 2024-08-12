package com.braintreepayments.api.card

import android.os.Parcel
import com.braintreepayments.api.testutils.CardNumber.VISA
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.PaymentMethod
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class CardUnitTest {

    private val CREDIT_CARD_KEY = "creditCard"
    private val BILLING_ADDRESS_KEY = "billingAddress"

    private val GRAPH_QL_MUTATION = "" +
            "mutation TokenizeCreditCard(\$input: TokenizeCreditCardInput!) {" +
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
            "    }" +
            "  }" +
            "}"

    private val GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED = "" +
            "mutation TokenizeCreditCard(\$input: TokenizeCreditCardInput!, " +
            "\$authenticationInsightInput: AuthenticationInsightInput!) {" +
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
            "    }" +
            "    authenticationInsight(input: \$authenticationInsightInput) {" +
            "      customerAuthenticationRegulationEnvironment" +
            "    }" +
            "  }" +
            "}"

    @Test
    fun buildJSON_correctlyBuildsACardTokenizationPayload() {
        val card = Card()
        card.number = VISA
        card.expirationMonth = "01"
        card.expirationYear = "2015"
        card.cvv = "123"
        card.cardholderName = "Joe Smith"
        card.firstName = "Joe"
        card.lastName = "Smith"
        card.company = "Company"
        card.streetAddress = "1 Main St"
        card.extendedAddress = "Unit 1"
        card.locality = "Some Town"
        card.postalCode = "12345"
        card.region = "Some Region"
        card.countryCode = "USA"
        card.shouldValidate = true
        card.merchantAccountId = "merchant-account-id"
        card.isAuthenticationInsightRequested = true
        card.setIntegration(IntegrationType.CUSTOM)
        card.setSource("test-source")
        card.setSessionId("test-session-id")

        val json = card.buildJSON()
        val jsonCard = json?.getJSONObject(CREDIT_CARD_KEY)
        val jsonBillingAddress = jsonCard?.getJSONObject(BILLING_ADDRESS_KEY)
        val jsonMetadata = json?.getJSONObject(MetadataBuilder.META_KEY)

        assertEquals(VISA, jsonCard?.getString("number"))
        assertEquals("01", jsonCard?.getString("expirationMonth"))
        assertEquals("2015", jsonCard?.getString("expirationYear"))
        assertEquals("123", jsonCard?.getString("cvv"))
        assertEquals("Joe Smith", jsonCard?.getString("cardholderName"))

        assertTrue(json?.getBoolean("authenticationInsight") == true)
        assertEquals("merchant-account-id", json?.getString("merchantAccountId"))

        assertTrue(
            jsonCard?.getJSONObject(PaymentMethod.OPTIONS_KEY)?.getBoolean("validate") == true
        )

        assertEquals("Joe", jsonBillingAddress?.getString("firstName"))
        assertEquals("Smith", jsonBillingAddress?.getString("lastName"))
        assertEquals("Company", jsonBillingAddress?.getString("company"))
        assertEquals("1 Main St", jsonBillingAddress?.getString("streetAddress"))
        assertEquals("Unit 1", jsonBillingAddress?.getString("extendedAddress"))
        assertEquals("Some Town", jsonBillingAddress?.getString("locality"))
        assertEquals("12345", jsonBillingAddress?.getString("postalCode"))
        assertEquals("Some Region", jsonBillingAddress?.getString("region"))
        assertEquals("USA", jsonBillingAddress?.getString("countryCodeAlpha3"))

        assertEquals(IntegrationType.CUSTOM.stringValue, jsonMetadata?.getString("integration"))
        assertEquals("test-source", jsonMetadata?.getString("source"))
        assertEquals("test-session-id", jsonMetadata?.getString("sessionId"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_nestsAddressCorrectly() {
        val card = Card()
        card.postalCode = "60606"

        val billingAddress = card.buildJSON()
            ?.getJSONObject(CREDIT_CARD_KEY)
            ?.getJSONObject(BILLING_ADDRESS_KEY)

        billingAddress?.let {
            assertFalse(billingAddress.has("firstName"))
            assertFalse(billingAddress.has("lastName"))
            assertFalse(billingAddress.has("company"))
            assertFalse(billingAddress.has("streetAddress"))
            assertFalse(billingAddress.has("extendedAddress"))
            assertFalse(billingAddress.has("locality"))
            assertEquals("60606", billingAddress.getString("postalCode"))
            assertFalse(billingAddress.has("region"))
            assertFalse(billingAddress.has("countryCode"))
            assertFalse(billingAddress.has("countryName"))
            assertFalse(billingAddress.has("countryCodeAlpha2"))
            assertFalse(billingAddress.has("countryCodeAlpha3"))
            assertFalse(billingAddress.has("countryCodeNumeric"))
        }
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_usesDefaultInfoForMetadata() {
        val card = Card()

        val metadata = card.buildJSON()
            ?.getJSONObject(MetadataBuilder.META_KEY)

        metadata?.let {
            assertEquals("custom", metadata.getString("integration"))
            assertEquals("form", metadata.getString("source"))
        }
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_usesDefaultCardSource() {
        val card = Card()
        val jsonObject = card.buildJSON()

        assertEquals("form",
            jsonObject?.getJSONObject("_meta")?.getString("source"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_setsCardSource() {
        val card = Card()
        card.setSource("form")

        val jsonObject = card.buildJSON()

        assertEquals("form",
            jsonObject?.getJSONObject("_meta")?.getString("source"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_setsIntegrationMethod() {
        val card = Card()
        card.setIntegration(IntegrationType.CUSTOM)

        val metadata = card.buildJSON()?.getJSONObject(MetadataBuilder.META_KEY)

        assertEquals(IntegrationType.CUSTOM.stringValue, metadata?.getString("integration"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_whenValidateIsNotSet_defaultsToFalse() {
        val card = Card()
        val json = card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)

        assertFalse(json?.getJSONObject("options")?.getBoolean("validate") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_includesValidateOptionWhenSetToTrue() {
        val card = Card()
        card.shouldValidate = true

        val json = card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)

        assertTrue(json?.getJSONObject("options")?.getBoolean("validate") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_includesValidateOptionWhenSetToFalse() {
        val card = Card()
        card.shouldValidate = false

        val builtCard = card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)

        assertFalse(builtCard?.getJSONObject("options")?.getBoolean("validate") == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_doesNotIncludeEmptyStrings() {
        val card = Card()
        card.number = ""
        card.expirationDate = ""
        card.expirationMonth = ""
        card.expirationYear = ""
        card.cvv = ""
        card.postalCode = ""
        card.cardholderName = ""
        card.firstName = ""
        card.lastName = ""
        card.company = ""
        card.streetAddress = ""
        card.extendedAddress = ""
        card.locality = ""
        card.postalCode = ""
        card.region = ""
        card.countryCode = ""

        assertEquals(1, card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)?.length())
        assertTrue(card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)?.has("options") == true)
        assertFalse(card.buildJSON()?.has(BILLING_ADDRESS_KEY) == true)
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_whenAuthenticationInsightRequestedIsTrue_requestsAuthenticationInsight() {
        val card = Card()
        card.isAuthenticationInsightRequested = true
        card.merchantAccountId = "merchant_account_id"

        val json = card.buildJSON()

        assertTrue(json?.getBoolean("authenticationInsight") == true)
        assertEquals("merchant_account_id", json?.getString("merchantAccountId"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_whenAuthenticationInsightRequestedIsFalse_doesNotRequestsAuthenticationInsight() {
        val card = Card()
        card.isAuthenticationInsightRequested = false

        val json = card.buildJSON()

        assertFalse(json?.has("authenticationInsight") == true)
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_correctlyBuildsACardTokenization() {
        val card = Card()
        card.number = VISA
        card.expirationMonth = "01"
        card.expirationYear = "2015"
        card.cvv = "123"
        card.cardholderName = "Joe Smith"
        card.firstName = "Joe"
        card.lastName = "Smith"
        card.company = "Company"
        card.streetAddress = "1 Main St"
        card.extendedAddress = "Unit 1"
        card.locality = "Some Town"
        card.postalCode = "12345"
        card.region = "Some Region"
        card.countryCode = "USA"
        card.shouldValidate = true
        card.merchantAccountId = "merchant-account-id"
        card.isAuthenticationInsightRequested = true
        card.setIntegration(IntegrationType.CUSTOM)
        card.setSource("test-source")
        card.setSessionId("test-session-id")

        val json = card.buildJSONForGraphQL()
        val jsonCard = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)
            .getJSONObject(BaseCard.CREDIT_CARD_KEY)
        val jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY)
        val jsonOptions = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)
            .getJSONObject(PaymentMethod.OPTIONS_KEY)
        val jsonMetadata = json.getJSONObject("clientSdkMetadata")

        assertEquals(GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED,
            json.getString(GraphQLConstants.Keys.QUERY))
        assertEquals(VISA, jsonCard.getString("number"))
        assertEquals("01", jsonCard.getString("expirationMonth"))
        assertEquals("2015", jsonCard.getString("expirationYear"))
        assertEquals("123", jsonCard.getString("cvv"))
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"))
        assertTrue(jsonOptions.getBoolean("validate"))
        assertEquals("Joe", jsonBillingAddress.getString("firstName"))
        assertEquals("Smith", jsonBillingAddress.getString("lastName"))
        assertEquals("Company", jsonBillingAddress.getString("company"))
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"))
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedAddress"))
        assertEquals("Some Town", jsonBillingAddress.getString("locality"))
        assertEquals("12345", jsonBillingAddress.getString("postalCode"))
        assertEquals("Some Region", jsonBillingAddress.getString("region"))
        assertEquals("USA", jsonBillingAddress.getString("countryCode"))
        assertEquals(IntegrationType.CUSTOM.stringValue, jsonMetadata.getString("integration"))
        assertEquals("test-source", jsonMetadata.getString("source"))
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_nestsAddressCorrectly() {
        val card = Card()
        card.postalCode = "60606"

        val json = card.buildJSONForGraphQL()
        val jsonCard = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
                .getJSONObject(GraphQLConstants.Keys.INPUT)
                .getJSONObject(BaseCard.CREDIT_CARD_KEY)
        val billingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY)

        assertFalse(billingAddress.has("firstName"))
        assertFalse(billingAddress.has("lastName"))
        assertFalse(billingAddress.has("company"))
        assertFalse(billingAddress.has("streetAddress"))
        assertFalse(billingAddress.has("extendedAddress"))
        assertFalse(billingAddress.has("locality"))
        assertEquals("60606", billingAddress.getString("postalCode"))
        assertFalse(billingAddress.has("region"))
        assertFalse(billingAddress.has("countryCode"))
        assertFalse(billingAddress.has("countryName"))
        assertFalse(billingAddress.has("countryCodeAlpha2"))
        assertFalse(billingAddress.has("countryCodeAlpha3"))
        assertFalse(billingAddress.has("countryCodeNumeric"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_usesDefaultInfoForMetadata() {
        val card = Card()

        val json = card.buildJSONForGraphQL()
        val metadata = json.getJSONObject("clientSdkMetadata")

        assertEquals("custom", metadata.getString("integration"))
        assertEquals("form", metadata.getString("source"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_usesDefaultCardSource() {
        val card = Card()

        val json = card.buildJSONForGraphQL()
        assertEquals("form",
            json.getJSONObject("clientSdkMetadata").getString("source"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_setsCardSource() {
        val card = Card()
        card.setSource("test-source")

        val json = card.buildJSONForGraphQL()

        assertEquals("test-source",
            json.getJSONObject("clientSdkMetadata").getString("source"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_setsIntegrationMethod() {
        val card = Card()
        card.setIntegration(IntegrationType.CUSTOM)

        val json = card.buildJSONForGraphQL()

        assertEquals(IntegrationType.CUSTOM.stringValue,
            json.getJSONObject("clientSdkMetadata").getString("integration"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_whenValidateNotSet_defaultsToFalse() {
        val card = Card()

        val json = card.buildJSONForGraphQL()
        val jsonOptions = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
                .getJSONObject(GraphQLConstants.Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY)

        assertFalse(jsonOptions.getBoolean("validate"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_whenValidateSetToTrue_includesValidationOptionTrue() {
        val card = Card()
        card.shouldValidate = true

        val json = card.buildJSONForGraphQL()
        val jsonOptions = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
                .getJSONObject(GraphQLConstants.Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY)

        assertTrue(jsonOptions.getBoolean("validate"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_whenValidateSetToFalse_includesValidationOptionFalse() {
        val card = Card()
        card.shouldValidate = false

        val json = card.buildJSONForGraphQL()
        val jsonOptions = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
                .getJSONObject(GraphQLConstants.Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY)

        assertFalse(jsonOptions.getBoolean("validate"))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_doesNotIncludeEmptyStrings() {
        val card = Card()
        card.number = ""
        card.expirationDate = ""
        card.expirationMonth = ""
        card.expirationYear = ""
        card.cvv = ""
        card.postalCode = ""
        card.cardholderName = ""
        card.firstName = ""
        card.lastName = ""
        card.company = ""
        card.streetAddress = ""
        card.extendedAddress = ""
        card.locality = ""
        card.postalCode = ""
        card.region = ""
        card.countryCode = ""

        val json = card.buildJSONForGraphQL()
        val jsonCard = json.getJSONObject(GraphQLConstants.Keys.VARIABLES)
                .getJSONObject(GraphQLConstants.Keys.INPUT)
                .getJSONObject(BaseCard.CREDIT_CARD_KEY)

        assertFalse(jsonCard.keys().hasNext())
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_withMerchantAccountId_andAuthInsightRequested_requestsAuthInsight() {
        val card = Card()
        card.merchantAccountId = "merchant-account-id"
        card.isAuthenticationInsightRequested = true

        val json = card.buildJSONForGraphQL()
        val variablesJson = json.optJSONObject(GraphQLConstants.Keys.VARIABLES)

        assertEquals(variablesJson!!
            .getJSONObject("authenticationInsightInput")["merchantAccountId"],
            "merchant-account-id")
        assertEquals(GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED,
            json.getString(GraphQLConstants.Keys.QUERY))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_withMerchantAccountId_andNoAuthInsightRequested_doesNotRequestInsight() {
        val card = Card()
        card.merchantAccountId = "merchant-account-id"
        card.isAuthenticationInsightRequested = false

        val json = card.buildJSONForGraphQL()
        val variablesJson = json.optJSONObject(GraphQLConstants.Keys.VARIABLES)

        assertNull(variablesJson?.optJSONObject("authenticationInsightInput"))
        assertEquals(GRAPH_QL_MUTATION, json.getString(GraphQLConstants.Keys.QUERY))
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_whenMerchantAccountIdIsNull_andAuthInsightRequested_throwsException() {
        val card = Card()
        card.merchantAccountId = null
        card.isAuthenticationInsightRequested = true

        val expectedException = assertFailsWith<BraintreeException> {
            card.buildJSONForGraphQL()
        }

        assertEquals("A merchant account ID is required when " +
                "authenticationInsightRequested is true.", expectedException.message)
    }

    @Test
    @Throws(Exception::class)
    fun buildJSONForGraphQL_withNullMerchantAccountId_andNoInsightRequested_doesNotRequestInsight() {
        val card = Card()
        card.merchantAccountId = null
        card.isAuthenticationInsightRequested = false

        val json = card.buildJSONForGraphQL()
        val variablesJson = json.optJSONObject(GraphQLConstants.Keys.VARIABLES)

        assertNull(variablesJson?.optJSONObject("authenticationInsightInput"))
        assertEquals(GRAPH_QL_MUTATION, json.getString(GraphQLConstants.Keys.QUERY))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_handlesFullExpirationDateMMYY() {
        val card = Card()
        card.expirationDate = "01/15"

        val jsonCard = card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)

        assertEquals("01", jsonCard?.getString("expirationMonth"))
        assertEquals("15", jsonCard?.getString("expirationYear"))
    }

    @Test
    @Throws(JSONException::class)
    fun buildJSON_handlesFullExpirationDateMMYYYY() {
        val card = Card()
        card.expirationDate = "01/2015"

        val jsonCard = card.buildJSON()?.getJSONObject(CREDIT_CARD_KEY)

        assertEquals("01", jsonCard?.getString("expirationMonth"))
        assertEquals("2015", jsonCard?.getString("expirationYear"))
    }

    @Test
    @Throws(Exception::class)
    fun parcelsCorrectly() {
        val card = Card()
        card.number = VISA
        card.expirationMonth = "01"
        card.expirationYear = "2015"
        card.cvv = "123"
        card.cardholderName = "Joe Smith"
        card.firstName = "Joe"
        card.lastName = "Smith"
        card.company = "Company"
        card.streetAddress = "1 Main St"
        card.extendedAddress = "Unit 1"
        card.locality = "Some Town"
        card.postalCode = "12345"
        card.region = "Some Region"
        card.countryCode = "USA"
        card.shouldValidate = true
        card.merchantAccountId = "merchant-account-id"
        card.isAuthenticationInsightRequested = true
        card.setIntegration(IntegrationType.CUSTOM)
        card.setSource("test-source")
        card.setSessionId("test-session-id")

        val parcel = Parcel.obtain()
        card.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val actual = Card.CREATOR.createFromParcel(parcel)

        val toParcelJson = card.buildJSONForGraphQL()
        val fromParcelJson = actual.buildJSONForGraphQL()

        assertEquals(toParcelJson.toString(), fromParcelJson.toString())
    }

    @Test
    @Throws(IllegalAccessException::class)
    fun parcelsCountryCodeCorrectly() {
        val card = Card()
        card.countryCode = "USA"

        val parcel = Parcel.obtain()
        card.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val actual = Card.CREATOR.createFromParcel(parcel)

        assertEquals("USA", actual.countryCode)
    }
}
