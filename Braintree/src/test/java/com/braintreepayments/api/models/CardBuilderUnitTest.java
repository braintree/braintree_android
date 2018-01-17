package com.braintreepayments.api.models;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Parcel;

import com.braintreepayments.api.R;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.internal.GraphQLQueryHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class CardBuilderUnitTest {

    private static final String CREDIT_CARD_KEY = "creditCard";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";

    @Test
    public void build_correctlyBuildsACard() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2015")
                .cvv("123")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .company("Company")
                .streetAddress("1 Main St")
                .extendedAddress("Unit 1")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryCode("USA")
                .countryName("Some Country")
                .countryCodeAlpha2("US")
                .countryCodeAlpha3("RUS")
                .countryCodeNumeric("840")
                .integration("test-integration")
                .source("test-source")
                .validate(true)
                .setSessionId("test-session-id");

        JSONObject json = new JSONObject(cardBuilder.build());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertTrue(jsonCard.getJSONObject(PaymentMethodBuilder.OPTIONS_KEY).getBoolean("validate"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("Some Country", jsonBillingAddress.getString("countryName"));
        assertEquals("US", jsonBillingAddress.getString("countryCodeAlpha2"));
        assertEquals("USA", jsonBillingAddress.getString("countryCodeAlpha3"));
        assertEquals("840", jsonBillingAddress.getString("countryCodeNumeric"));

        assertEquals("test-integration", jsonMetadata.getString("integration"));
        assertEquals("test-source", jsonMetadata.getString("source"));
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"));
    }

    @Test
    public void build_nestsAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        JSONObject billingAddress = new JSONObject(cardBuilder.build())
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

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void build_usesDefaultCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder();
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder().source("form");
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void build_setsIntegrationMethod() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().integration("test-integration");

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToTrue() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().validate(true);

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(true, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_includesValidateOptionWhenSetToFalse() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().validate(false);

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(false, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void build_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        assertFalse(new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void build_doesNotIncludeEmptyStrings() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("")
                .expirationDate("")
                .expirationMonth("")
                .expirationYear("")
                .cvv("")
                .postalCode("")
                .cardholderName("")
                .firstName("")
                .lastName("")
                .company("")
                .streetAddress("")
                .extendedAddress("")
                .locality("")
                .postalCode("")
                .region("")
                .countryCode("")
                .countryName("")
                .countryCodeAlpha2("")
                .countryCodeAlpha3("")
                .countryCodeNumeric("");

        assertFalse(new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildGraphQL_correctlyBuildsACardTokenization() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2015")
                .cvv("123")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .company("Company")
                .streetAddress("1 Main St")
                .extendedAddress("Unit 1")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryCode("USA")
                .countryName("Some Country")
                .countryCodeAlpha2("US")
                .countryCodeAlpha3("RUS")
                .countryCodeNumeric("840")
                .integration("test-integration")
                .source("test-source")
                .validate(true)
                .setSessionId("test-session-id");

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(BaseCardBuilder.CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonOptions = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);
        JSONObject jsonMetadata = json.getJSONObject("clientSdkMetadata");

        assertEquals(GraphQLQueryHelper.getQuery(context, R.raw.tokenize_credit_card_mutation),
                json.getString(GraphQLQueryHelper.QUERY_KEY));

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
        assertEquals("Some Country", jsonBillingAddress.getString("countryName"));
        assertEquals("US", jsonBillingAddress.getString("countryCodeAlpha2"));
        assertEquals("RUS", jsonBillingAddress.getString("countryCodeAlpha3"));
        assertEquals("840", jsonBillingAddress.getString("countryCodeNumeric"));

        assertEquals("test-integration", jsonMetadata.getString("integration"));
        assertEquals("test-source", jsonMetadata.getString("source"));
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"));
    }

    @Test
    public void buildGraphQL_nestsAddressCorrectly() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
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

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject metadata = json.getJSONObject("clientSdkMetadata");

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void buildGraphQL_usesDefaultCardSource() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));

        assertEquals("form", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsCardSource() throws Exception {
        CardBuilder cardBuilder = new CardBuilder().source("test-source");

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));

        assertEquals("test-source", json.getJSONObject("clientSdkMetadata").getString("source"));
    }

    @Test
    public void buildGraphQL_setsIntegrationMethod() throws Exception {
        CardBuilder cardBuilder = new CardBuilder().integration("test-integration");

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));

        assertEquals("test-integration", json.getJSONObject("clientSdkMetadata").getString("integration"));
    }

    @Test
    public void buildGraphQL_includesValidateOptionWhenSetToTrue() throws Exception {
        CardBuilder cardBuilder = new CardBuilder().validate(true);

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertTrue(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_includesValidateOptionWhenSetToFalse() throws Exception {
        CardBuilder cardBuilder = new CardBuilder().validate(false);

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertFalse(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_defaultsValidateToTrueForClientTokens() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(
                cardBuilder.buildGraphQL(context, Authorization.fromString(stringFromFixture("client_token.json"))));
        JSONObject jsonOptions = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertTrue(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_defaultsValidateToFalseForTokenizationKeys() throws Exception {
        CardBuilder cardBuilder = new CardBuilder();

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonOptions = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(PaymentMethodBuilder.OPTIONS_KEY);

        assertFalse(jsonOptions.getBoolean("validate"));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        assertFalse(new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void buildGraphQL_doesNotIncludeEmptyStrings() throws Exception {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("")
                .expirationDate("")
                .expirationMonth("")
                .expirationYear("")
                .cvv("")
                .postalCode("")
                .cardholderName("")
                .firstName("")
                .lastName("")
                .company("")
                .streetAddress("")
                .extendedAddress("")
                .locality("")
                .postalCode("")
                .region("")
                .countryCode("")
                .countryName("")
                .countryCodeAlpha2("")
                .countryCodeAlpha3("")
                .countryCodeNumeric("");

        Context context = RuntimeEnvironment.application.getApplicationContext();
        JSONObject json = new JSONObject(cardBuilder.buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY)));
        JSONObject jsonCard = json.getJSONObject(GraphQLQueryHelper.VARIABLES_KEY)
                .getJSONObject(GraphQLQueryHelper.INPUT_KEY)
                .getJSONObject(BaseCardBuilder.CREDIT_CARD_KEY);

        assertFalse(jsonCard.keys().hasNext());
    }

    @Test
    public void buildGraphQL_throwsAnExceptionWhenTheQueryCannotBeRead() throws InvalidArgumentException {
        Resources resources = mock(Resources.class);
        doThrow(new NotFoundException("Not found")).when(resources).openRawResource(anyInt());
        Context context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);

        try {
            new CardBuilder().buildGraphQL(context, Authorization.fromString(TOKENIZATION_KEY));
            fail("Expected exception");
        } catch (BraintreeException e) {
            assertEquals("Unable to read GraphQL query", e.getMessage());
            assertEquals("Not found", e.getCause().getMessage());
        }
    }

    @Test
    public void handlesFullExpirationDateMMYY() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().expirationDate("01/15");

        JSONObject jsonCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("15", jsonCard.getString("expirationYear"));
    }

    @Test
    public void handlesFullExpirationDateMMYYYY() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().expirationDate("01/2015");

        JSONObject jsonCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("01")
                .expirationYear("2015")
                .cvv("123")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .company("Company")
                .streetAddress("1 Main St")
                .extendedAddress("Unit 1")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryName("Some Country")
                .countryCodeAlpha2("US")
                .countryCodeAlpha3("USA")
                .countryCodeNumeric("840")
                .integration("test-integration")
                .source("test-source")
                .validate(true)
                .setSessionId("test-session-id");

        Parcel parcel = Parcel.obtain();
        cardBuilder.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardBuilder actual = CardBuilder.CREATOR.createFromParcel(parcel);

        JSONObject json = new JSONObject(actual.build());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertTrue(jsonCard.getJSONObject(PaymentMethodBuilder.OPTIONS_KEY).getBoolean("validate"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("Some Country", jsonBillingAddress.getString("countryName"));
        assertEquals("US", jsonBillingAddress.getString("countryCodeAlpha2"));
        assertEquals("USA", jsonBillingAddress.getString("countryCodeAlpha3"));
        assertEquals("840", jsonBillingAddress.getString("countryCodeNumeric"));

        assertEquals("test-integration", jsonMetadata.getString("integration"));
        assertEquals("test-source", jsonMetadata.getString("source"));
        assertEquals("test-session-id", jsonMetadata.getString("sessionId"));
    }

    @Test
    public void parcelsCountryCodeCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().countryCode("USA");

        Parcel parcel = Parcel.obtain();
        cardBuilder.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardBuilder actual = CardBuilder.CREATOR.createFromParcel(parcel);

        JSONObject json = new JSONObject(actual.build());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);

        assertEquals("USA", jsonBillingAddress.getString("countryCode"));
    }
}
