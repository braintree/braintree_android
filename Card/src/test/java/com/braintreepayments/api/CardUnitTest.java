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
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardUnitTest {
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
        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("01");
        card.setExpirationYear("2015");
        card.setCvv("123");
        card.setCardholderName("Joe Smith");
        card.setFirstName("Joe");
        card.setLastName("Smith");
        card.setCompany("Company");
        card.setStreetAddress("1 Main St");
        card.setExtendedAddress("Unit 1");
        card.setLocality("Some Town");
        card.setPostalCode("12345");
        card.setRegion("Some Region");
        card.setCountryCode("USA");
        card.setIntegration("test-integration");
        card.setSource("test-source");
        card.setShouldValidate(true);
        card.setSessionId("test-session-id");
        card.setMerchantAccountId("merchant-account-id");
        card.setAuthenticationInsightRequested(true);

        JSONObject json = card.buildJSON();
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

        assertTrue(jsonCard.getJSONObject(PaymentMethod.OPTIONS_KEY).getBoolean("validate"));

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
        Card card = new Card();
        card.setPostalCode("60606");

        JSONObject billingAddress = card.buildJSON()
                .getJSONObject(CREDIT_CARD_KEY)
                .getJSONObject(BILLING_ADDRESS_KEY);

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
        Card card = new Card();

        JSONObject metadata = card.buildJSON()
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void build_usesDefaultCardSource() throws JSONException {
        Card card = new Card();
        JSONObject jsonObject = card.buildJSON();

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsCardSource() throws JSONException {
        Card card = new Card();
        card.setSource("form");
        JSONObject jsonObject = card.buildJSON();

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsIntegrationMethod() throws JSONException {
        Card card = new Card();
        card.setIntegration("test-integration");

        JSONObject metadata = card.buildJSON()
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToTrue() throws JSONException {
        Card card = new Card();
        card.setShouldValidate(true);

        JSONObject builtCard = card.buildJSON()
                .getJSONObject(CREDIT_CARD_KEY);

        assertTrue(builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToFalse() throws JSONException {
        Card card = new Card();
        card.setShouldValidate(false);

        JSONObject builtCard = card.buildJSON()
                .getJSONObject(CREDIT_CARD_KEY);

        assertFalse(builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        Card card = new Card();

        assertFalse(card.buildJSON().getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(card.buildJSON().has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void build_doesNotIncludeEmptyStrings() throws JSONException {
        Card card = new Card();
        card.setNumber("");
        card.setExpirationDate("");
        card.setExpirationMonth("");
        card.setExpirationYear("");
        card.setCvv("");
        card.setPostalCode("");
        card.setCardholderName("");
        card.setFirstName("");
        card.setLastName("");
        card.setCompany("");
        card.setStreetAddress("");
        card.setExtendedAddress("");
        card.setLocality("");
        card.setPostalCode("");
        card.setRegion("");
        card.setCountryCode("");

        assertFalse(card.buildJSON().getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(card.buildJSON().has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void build_whenAuthenticationInsightRequestedIsTrue_requestsAuthenticationInsight() throws JSONException {
        Card card = new Card();
        card.setAuthenticationInsightRequested(true);
        card.setMerchantAccountId("merchant_account_id");

        JSONObject json = card.buildJSON();

        assertTrue(json.getBoolean("authenticationInsight"));
        assertEquals("merchant_account_id", json.getString("merchantAccountId"));
    }

    @Test
    public void build_whenAuthenticationInsightRequestedIsFalse_doesNotRequestsAuthenticationInsight() {
        Card card = new Card();
        card.setAuthenticationInsightRequested(false);

        JSONObject json = card.buildJSON();
        assertFalse(json.has("authenticationInsight"));
    }

    @Test
    public void buildGraphQL_correctlyBuildsACardTokenization() throws Exception {
        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("01");
        card.setExpirationYear("2015");
        card.setCvv("123");
        card.setCardholderName("Joe Smith");
        card.setFirstName("Joe");
        card.setLastName("Smith");
        card.setCompany("Company");
        card.setStreetAddress("1 Main St");
        card.setExtendedAddress("Unit 1");
        card.setLocality("Some Town");
        card.setPostalCode("12345");
        card.setRegion("Some Region");
        card.setCountryCode("USA");
        card.setIntegration("test-integration");
        card.setSource("test-source");
        card.setShouldValidate(true);
        card.setSessionId("test-session-id");
        card.setMerchantAccountId("merchant-account-id");
        card.setAuthenticationInsightRequested(true);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCard.CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY);
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
        Card card = new Card();
        card.setPostalCode("60606");

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCard.CREDIT_CARD_KEY);
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
        Card card = new Card();

        JSONObject json = card.buildGraphQLJSON();
        JSONObject metadata = json.getJSONObject("clientSdkMetadata");

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void buildGraphQL_usesDefaultCardSource() throws Exception {
        Card card = new Card();

        JSONObject json = card.buildGraphQLJSON();
        assertEquals("form", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsCardSource() throws Exception {
        Card card = new Card();
        card.setSource("test-source");

        JSONObject json = card.buildGraphQLJSON();
        assertEquals("test-source", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsIntegrationMethod() throws Exception {
        Card card = new Card();
        card.setIntegration("test-integration");

        JSONObject json = card.buildGraphQLJSON();
        assertEquals("test-integration", json.getJSONObject("clientSdkMetadata").getString("integration"));
    }

    @Test
    public void buildGraphQL_whenValidateNotSet_doesNotIncludeValidateOption() throws Exception {
        Card card = new Card();

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY);

        assertFalse(jsonOptions.has("validate"));
    }

    @Test
    public void buildGraphQL_whenValidateSetToTrue_includesValidationOptionTrue() throws Exception {
        Card card = new Card();
        card.setShouldValidate(true);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY);

        assertTrue(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_whenValidateSetToFalse_includesValidationOptionFalse() throws Exception {
        Card card = new Card();
        card.setShouldValidate(false);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonOptions = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(PaymentMethod.OPTIONS_KEY);

        assertFalse(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        Card card = new Card();

        assertFalse(card.buildJSON().getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(card.buildJSON().has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyStrings() throws Exception {
        Card card = new Card();
        card.setNumber("");
        card.setExpirationDate("");
        card.setExpirationMonth("");
        card.setExpirationYear("");
        card.setCvv("");
        card.setPostalCode("");
        card.setCardholderName("");
        card.setFirstName("");
        card.setLastName("");
        card.setCompany("");
        card.setStreetAddress("");
        card.setExtendedAddress("");
        card.setLocality("");
        card.setPostalCode("");
        card.setRegion("");
        card.setCountryCode("");

        JSONObject json = card.buildGraphQLJSON();
        JSONObject jsonCard = json.getJSONObject(Keys.VARIABLES)
                .getJSONObject(Keys.INPUT)
                .getJSONObject(BaseCard.CREDIT_CARD_KEY);

        assertFalse(jsonCard.keys().hasNext());
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsPresent_andAuthInsightRequestedIsTrue_requestsAuthInsight() throws Exception {
        Card card = new Card();
        card.setMerchantAccountId("merchant-account-id");
        card.setAuthenticationInsightRequested(true);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertEquals(variablesJson.getJSONObject("authenticationInsightInput")
                .get("merchantAccountId"), "merchant-account-id");

        assertEquals(GRAPH_QL_MUTATION_WITH_AUTH_INSIGHT_REQUESTED, json.getString(Keys.QUERY));
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsPresent_andAuthInsightRequestedIsFalse_doesNotRequestAuthInsight() throws Exception {
        Card card = new Card();
        card.setMerchantAccountId("merchant-account-id");
        card.setAuthenticationInsightRequested(false);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertNull(variablesJson.optJSONObject("authenticationInsightInput"));

        assertEquals(GRAPH_QL_MUTATION, json.getString(Keys.QUERY));
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsNull_andAuthInsightRequestedIsTrue_throwsException() throws Exception {
        Card card = new Card();
        card.setMerchantAccountId(null);
        card.setAuthenticationInsightRequested(true);

        exceptionRule.expect(BraintreeException.class);
        exceptionRule.expectMessage("A merchant account ID is required when authenticationInsightRequested is true.");
        card.buildGraphQLJSON();
    }

    @Test
    public void buildGraphQL_whenMerchantAccountIdIsNull_andAuthInsightRequestedIsFalse_doesNotRequestAuthInsight() throws Exception {
        Card card = new Card();
        card.setMerchantAccountId(null);
        card.setAuthenticationInsightRequested(false);

        JSONObject json = card.buildGraphQLJSON();
        JSONObject variablesJson = json.optJSONObject(Keys.VARIABLES);

        assertNull(variablesJson.optJSONObject("authenticationInsightInput"));

        assertEquals(GRAPH_QL_MUTATION, json.getString(Keys.QUERY));
    }

    @Test
    public void handlesFullExpirationDateMMYY() throws JSONException {
        Card card = new Card();
        card.setExpirationDate("01/15");

        JSONObject jsonCard = card.buildJSON().getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("15", jsonCard.getString("expirationYear"));
    }

    @Test
    public void handlesFullExpirationDateMMYYYY() throws JSONException {
        Card card = new Card();
        card.setExpirationDate("01/2015");

        JSONObject jsonCard = card.buildJSON().getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
    }

    @Test
    public void parcelsCorrectly() throws Exception {
        Card card = new Card();
        card.setNumber(VISA);
        card.setExpirationMonth("01");
        card.setExpirationYear("2015");
        card.setCvv("123");
        card.setCardholderName("Joe Smith");
        card.setFirstName("Joe");
        card.setLastName("Smith");
        card.setCompany("Company");
        card.setStreetAddress("1 Main St");
        card.setExtendedAddress("Unit 1");
        card.setLocality("Some Town");
        card.setPostalCode("12345");
        card.setRegion("Some Region");
        card.setCountryCode("USA");
        card.setIntegration("test-integration");
        card.setSource("test-source");
        card.setShouldValidate(true);
        card.setSessionId("test-session-id");
        card.setMerchantAccountId("merchant-account-id");
        card.setAuthenticationInsightRequested(true);

        Parcel parcel = Parcel.obtain();
        card.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Card actual = Card.CREATOR.createFromParcel(parcel);

        JSONObject toParcelJson = card.buildGraphQLJSON();
        JSONObject fromParcelJson = actual.buildGraphQLJSON();

        assertEquals(toParcelJson.toString(), fromParcelJson.toString());
    }

    @Test
    public void parcelsCountryCodeCorrectly() throws IllegalAccessException {
        Card card = new Card();
        card.setCountryCode("USA");

        Parcel parcel = Parcel.obtain();
        card.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Card actual = Card.CREATOR.createFromParcel(parcel);

        assertEquals("USA", ReflectionHelper.getField("countryCode", actual));
    }
}
