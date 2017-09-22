package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class CardBuilderUnitTest {

    private static final String CREDIT_CARD_KEY = "creditCard";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";

    @Test
    public void build_correctlyBuildsACard() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber(VISA)
                .expirationDate("01/2015")
                .expirationMonth("01")
                .expirationYear("2015")
                .cvv("123")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .company("Company")
                .company("Company")
                .streetAddress("1 Main St")
                .extendedStreetAddress("Unit 1")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryName("Some Country")
                .countryCodeAlpha2("US")
                .countryCodeAlpha3("USA")
                .countryCodeNumeric("840");

        JSONObject json = new JSONObject(cardBuilder.build());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("01/2015", jsonCard.getString("expirationDate"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("Company", jsonBillingAddress.getString("company"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Unit 1", jsonBillingAddress.getString("extendedStreetAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("Some Country", jsonBillingAddress.getString("countryName"));
        assertEquals("US", jsonBillingAddress.getString("countryCodeAlpha2"));
        assertEquals("USA", jsonBillingAddress.getString("countryCodeAlpha2"));
        assertEquals("840", jsonBillingAddress.getString("countryCodeNumeric"));

        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("form", jsonMetadata.getString("source"));
    }

    @Test
    public void handlesFullExpirationDate() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber(VISA)
                .cvv("123")
                .expirationDate("01/15")
                .postalCode("12345");

        JSONObject jsonCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("01/15", jsonCard.getString("expirationDate"));
        assertEquals("12345", jsonCard.getJSONObject("billingAddress").getString("postalCode"));
    }

    @Test
    public void nestsAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        JSONObject billingAddress = new JSONObject(cardBuilder.build())
                .getJSONObject(CREDIT_CARD_KEY).getJSONObject(BILLING_ADDRESS_KEY);

        assertFalse(billingAddress.has("firstName"));
        assertFalse(billingAddress.has("lastName"));
        assertFalse(billingAddress.has("company"));
        assertFalse(billingAddress.has("streetAddress"));
        assertFalse(billingAddress.has("extendedStreetAddress"));
        assertFalse(billingAddress.has("locality"));
        assertEquals("60606", billingAddress.getString("postalCode"));
        assertFalse(billingAddress.has("region"));
        assertFalse(billingAddress.has("countryName"));
        assertFalse(billingAddress.has("countryCodeAlpha2"));
        assertFalse(billingAddress.has("countryCodeAlpha3"));
        assertFalse(billingAddress.has("countryCodeNumeric"));
    }

    @Test
    public void usesDefaultInfoForMetadata() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test
    public void usesDefaultCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder();
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void setsCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder().source("form");
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test
    public void setsIntegrationMethod() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().integration("test-integration");

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test
    public void includesValidateOptionWhenSetToTrue() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().validate(true);

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(true, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void includesValidateOptionWhenSetToFalse() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().validate(false);

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(false, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        assertFalse(new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }

    @Test
    public void doesNotIncludeEmptyStrings() throws JSONException {
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
                .extendedStreetAddress("")
                .locality("")
                .postalCode("")
                .region("")
                .countryName("")
                .countryCodeAlpha2("")
                .countryCodeAlpha3("")
                .countryCodeNumeric("");

        assertFalse(new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY).keys().hasNext());
        assertFalse(new JSONObject(cardBuilder.build()).has(BILLING_ADDRESS_KEY));
    }
}
