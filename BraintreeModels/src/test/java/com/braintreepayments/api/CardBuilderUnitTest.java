package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.CardNumber.VISA;
import static com.braintreepayments.api.FixturesHelper.base64Encode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardBuilderUnitTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private static final String CREDIT_CARD_KEY = "creditCard";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";

    private static final String GRAPH_QL_MUTATION = "" +
            "mutation TokenizeCreditCard($input: TokenizeCreditCardInput!) {" +
            "  tokenizeCreditCard(input: $input) {" +
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
            "}";

    private static final String GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED = "" +
            "mutation TokenizeCreditCard($input: TokenizeCreditCardInput!, $authenticationInsightInput: AuthenticationInsightInput!) {" +
            "  tokenizeCreditCard(input: $input) {" +
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
            "    authenticationInsight(input: $authenticationInsightInput) {" +
            "      customerAuthenticationRegulationEnvironment" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void build_correctlyBuildsACard() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.cardNumber(VISA);
        cardBuilder.expirationMonth("01");
        cardBuilder.expirationYear("2015");
        cardBuilder.cvv("123");
        cardBuilder.cardholderName("Joe Smith");
        cardBuilder.firstName("Joe");
        cardBuilder.lastName("Smith");
        cardBuilder.company("Company");
        cardBuilder.streetAddress("1 Main St");
        cardBuilder.extendedAddress("Unit 1");
        cardBuilder.locality("Some Town");
        cardBuilder.postalCode("12345");
        cardBuilder.region("Some Region");
        cardBuilder.countryCode("USA");
        cardBuilder.setIntegration("test-integration");
        cardBuilder.setSource("test-source");
        cardBuilder.setValidate(true);
        cardBuilder.setSessionId("test-session-id");
        cardBuilder.merchantAccountId("merchant-account-id");
        cardBuilder.authenticationInsightRequested(true);

        JSONObject json = new JSONObject(cardBuilder.buildJSON());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertTrue(json.getBoolean("authenticationInsight"));
        assertEquals("merchant-account-id", json.getString("merchantAccountId"));

        assertTrue(jsonCard.getJSONObject(PaymentMethodBuilder.OPTIONS_KEY).getBoolean("validate"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("USA", jsonBillingAddress.getString("countryCodeAlpha3"));

        assertEquals("test-integration", jsonMetadata.getString("integration"));
        assertEquals("test-source", jsonMetadata.getString("source"));
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"));
    }

    @Test
    public void build_nestsAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.postalCode("60606");

        JSONObject billingAddress = new JSONObject(cardBuilder.buildJSON())
                .getJSONObject(CREDIT_CARD_KEY).getJSONObject(BILLING_ADDRESS_KEY);

        assertFalse(billingAddress.has("firstName"));
        assertFalse(billingAddress.has("lastName"));
        assertFalse(billingAddress.has("company"));
        assertFalse(billingAddress.has("streetAddress"));
        assertFalse(billingAddress.has("extendedAddress"));
        assertFalse(billingAddress.has("locality"));
        assertEquals("60606", billingAddress.getString("postalCode"));
        assertFalse(billingAddress.has("region"));
        assertFalse(billingAddress.has("countryCode"));
        assertFalse(billingAddress.has("countryName"));
        assertFalse(billingAddress.has("countryCodeAlpha2"));
        assertFalse(billingAddress.has("countryCodeAlpha3"));
        assertFalse(billingAddress.has("countryCodeNumeric"));
    }

    @Test
    public void build_usesDefaultInfoForMetadata() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject metadata = new JSONObject(cardBuilder.buildJSON())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void build_usesDefaultCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder();
        JSONObject jsonObject = new JSONObject(builder.buildJSON());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsCardSource() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setSource("form");
        JSONObject jsonObject = new JSONObject(cardBuilder.buildJSON());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsIntegrationMethod() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setIntegration("test-integration");

        JSONObject metadata = new JSONObject(cardBuilder.buildJSON())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToTrue() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setValidate(true);

        JSONObject builtCard = new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY);

        assertTrue(builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToFalse() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setValidate(false);

        JSONObject builtCard = new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY);

        assertFalse(builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        assertFalse(new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.buildJSON()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void build_doesNotIncludeEmptyStrings() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.cardNumber("");
        cardBuilder.expirationDate("");
        cardBuilder.expirationMonth("");
        cardBuilder.expirationYear("");
        cardBuilder.cvv("");
        cardBuilder.postalCode("");
        cardBuilder.cardholderName("");
        cardBuilder.firstName("");
        cardBuilder.lastName("");
        cardBuilder.company("");
        cardBuilder.streetAddress("");
        cardBuilder.extendedAddress("");
        cardBuilder.locality("");
        cardBuilder.postalCode("");
        cardBuilder.region("");
        cardBuilder.countryCode("");

        assertFalse(new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.buildJSON()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void build_whenAuthenticationInsightRequestedIsTrue_requestsAuthenticationInsight() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.authenticationInsightRequested(true);
        cardBuilder.merchantAccountId("merchant_account_id");

        JSONObject json = new JSONObject(cardBuilder.buildJSON());

        assertTrue(json.getBoolean("authenticationInsight"));
        assertEquals("merchant_account_id", json.getString("merchantAccountId"));
    }

    @Test
    public void build_whenAuthenticationInsightRequestedIsFalse_doesNotRequestsAuthenticationInsight() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.authenticationInsightRequested(false);

        JSONObject json = new JSONObject(cardBuilder.buildJSON());

        assertFalse(json.has("authenticationInsight"));
    }

    @Test
    public void buildGraphQL_correctlyBuildsACardTokenization() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.cardNumber(VISA);
        cardBuilder.expirationMonth("01");
        cardBuilder.expirationYear("2015");
        cardBuilder.cvv("123");
        cardBuilder.cardholderName("Joe Smith");
        cardBuilder.firstName("Joe");
        cardBuilder.lastName("Smith");
        cardBuilder.company("Company");
        cardBuilder.streetAddress("1 Main St");
        cardBuilder.extendedAddress("Unit 1");
        cardBuilder.locality("Some Town");
        cardBuilder.postalCode("12345");
        cardBuilder.region("Some Region");
        cardBuilder.countryCode("USA");
        cardBuilder.setIntegration("test-integration");
        cardBuilder.setSource("test-source");
        cardBuilder.setValidate(true);
        cardBuilder.setSessionId("test-session-id");
        cardBuilder.merchantAccountId("merchant-account-id");
        cardBuilder.authenticationInsightRequested(true);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCardBuilder.CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);
        JSONObject jsonMetadata = json.getJSONObject("clientSdkMetadata");

        assertEquals(GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED, json.getString(Keys.QUERY));

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertTrue(jsonOptions.getBoolean("validate"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("USA", jsonBillingAddress.getString("countryCode"));

        assertEquals("test-integration", jsonMetadata.getString("integration"));
        assertEquals("test-source", jsonMetadata.getString("source"));
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"));
    }

    @Test
    public void buildGraphQL_nestsAddressCorrectly() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.postalCode("60606");

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCardBuilder.CREDIT_CARD_KEY);
        JSONObject billingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);

        assertFalse(billingAddress.has("firstName"));
        assertFalse(billingAddress.has("lastName"));
        assertFalse(billingAddress.has("company"));
        assertFalse(billingAddress.has("streetAddress"));
        assertFalse(billingAddress.has("extendedAddress"));
        assertFalse(billingAddress.has("locality"));
        assertEquals("60606", billingAddress.getString("postalCode"));
        assertFalse(billingAddress.has("region"));
        assertFalse(billingAddress.has("countryCode"));
        assertFalse(billingAddress.has("countryName"));
        assertFalse(billingAddress.has("countryCodeAlpha2"));
        assertFalse(billingAddress.has("countryCodeAlpha3"));
        assertFalse(billingAddress.has("countryCodeNumeric"));
    }

    @Test
    public void buildGraphQL_usesDefaultInfoForMetadata() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject metadata = json.getJSONObject("clientSdkMetadata");

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void buildGraphQL_usesDefaultCardSource() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));

        assertEquals("form", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsCardSource() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setSource("test-source");

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));

        assertEquals("test-source", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsIntegrationMethod() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setIntegration("test-integration");

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));

        assertEquals("test-integration", json.getJSONObject("clientSdkMetadata").getString("integration"));
    }

    @Test
    public void buildGraphQL_includesValidateOptionWhenSetToTrue() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setValidate(true);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertTrue(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_includesValidateOptionWhenSetToFalse() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.setValidate(false);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertFalse(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_defaultsValidateToTrueForClientTokens() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject json = new JSONObject(
                cardBuilder.buildGraphQL(Authorization.fromString(base64Encode(Fixtures.CLIENT_TOKEN))));
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertTrue(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_defaultsValidateToFalseForTokenizationKeys() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertFalse(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        assertFalse(new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.buildJSON()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyStrings() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.cardNumber("");
        cardBuilder.expirationDate("");
        cardBuilder.expirationMonth("");
        cardBuilder.expirationYear("");
        cardBuilder.cvv("");
        cardBuilder.postalCode("");
        cardBuilder.cardholderName("");
        cardBuilder.firstName("");
        cardBuilder.lastName("");
        cardBuilder.company("");
        cardBuilder.streetAddress("");
        cardBuilder.extendedAddress("");
        cardBuilder.locality("");
        cardBuilder.postalCode("");
        cardBuilder.region("");
        cardBuilder.countryCode("");

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCardBuilder.CREDIT_CARD_KEY);

        assertFalse(jsonCard.keys().hasNext());
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsPresent_andAuthInsightRequestedIsTrue_requestsAuthInsight() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.merchantAccountId("merchant-account-id");
        cardBuilder.authenticationInsightRequested(true);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertEquals(variablesJson.getJSONObject("authenticationInsightInput")
                .get("merchantAccountId"), "merchant-account-id");

        assertEquals(GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED, json.getString(Keys.QUERY));
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsPresent_andAuthInsightRequestedIsFalse_doesNotRequestAuthInsight() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.merchantAccountId("merchant-account-id");
        cardBuilder.authenticationInsightRequested(false);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertNull(variablesJson.optJSONObject("authenticationInsightInput"));

        assertEquals(GRAPH_QL_MUTATION, json.getString(Keys.QUERY));
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsNull_andAuthInsightRequestedIsTrue_throwsException() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.merchantAccountId(null);
        cardBuilder.authenticationInsightRequested(true);

        exceptionRule.expect(BraintreeException.class);
        exceptionRule.expectMessage("A merchant account ID is required when authenticationInsightRequested is true.");
        cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY));
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsNull_andAuthInsightRequestedIsFalse_doesNotRequestAuthInsight() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.merchantAccountId(null);
        cardBuilder.authenticationInsightRequested(false);

        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertNull(variablesJson.optJSONObject("authenticationInsightInput"));

        assertEquals(GRAPH_QL_MUTATION, json.getString(Keys.QUERY));
    }

    @Test
    public void handlesFullExpirationDateMMYY() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.expirationDate("01/15");

        JSONObject jsonCard = new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("15", jsonCard.getString("expirationYear"));
    }

    @Test
    public void handlesFullExpirationDateMMYYYY() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.expirationDate("01/2015");

        JSONObject jsonCard = new JSONObject(cardBuilder.buildJSON()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.cardNumber(VISA);
        cardBuilder.expirationMonth("01");
        cardBuilder.expirationYear("2015");
        cardBuilder.cvv("123");
        cardBuilder.cardholderName("Joe Smith");
        cardBuilder.firstName("Joe");
        cardBuilder.lastName("Smith");
        cardBuilder.company("Company");
        cardBuilder.streetAddress("1 Main St");
        cardBuilder.extendedAddress("Unit 1");
        cardBuilder.locality("Some Town");
        cardBuilder.postalCode("12345");
        cardBuilder.region("Some Region");
        cardBuilder.countryCode("USA");
        cardBuilder.setIntegration("test-integration");
        cardBuilder.setSource("test-source");
        cardBuilder.setValidate(true);
        cardBuilder.setSessionId("test-session-id");
        cardBuilder.merchantAccountId("merchant-account-id");
        cardBuilder.authenticationInsightRequested(true);

        Parcel parcel = Parcel.obtain();
        cardBuilder.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardBuilder actual = CardBuilder.CREATOR.createFromParcel(parcel);

        JSONObject toParcelJson = new JSONObject(cardBuilder.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));
        JSONObject fromParcelJson = new JSONObject(actual.buildGraphQL(Authorization.fromString(Fixtures.TOKENIZATION_KEY)));

        assertEquals(toParcelJson.toString(), fromParcelJson.toString());
    }

    @Test
    public void parcelsCountryCodeCorrectly() throws NoSuchFieldException, IllegalAccessException {
        CardBuilder cardBuilder = new CardBuilder();
        cardBuilder.countryCode("USA");

        Parcel parcel = Parcel.obtain();
        cardBuilder.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardBuilder actual = CardBuilder.CREATOR.createFromParcel(parcel);

        assertEquals("USA", ReflectionHelper.getField("mCountryCode", actual));
    }
}
