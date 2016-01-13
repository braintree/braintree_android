package com.braintreepayments.api.models;

import com.braintreepayments.api.VenmoAppSwitch;
import com.braintreepayments.api.models.PaymentMethod.Builder;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import static com.braintreepayments.testutils.CardNumber.VISA;

public class CardBuilderTest extends TestCase {

    private static final String CREDIT_CARD_KEY = "creditCard";
    public static final String BILLING_ADDRESS_KEY = "billingAddress";

    public void testBuildsACardCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber(VISA)
                .cvv("123")
                .expirationMonth("01")
                .expirationYear("2015")
                .cardholderName("Joe Smith")
                .firstName("Joe")
                .lastName("Smith")
                .streetAddress("1 Main St")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryName("Some Country");

        JSONObject json = new JSONObject(cardBuilder.toJsonString());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(Builder.METADATA_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));
        assertEquals("Joe Smith", jsonCard.getString("cardholderName"));

        assertEquals("Joe", jsonBillingAddress.getString("firstName"));
        assertEquals("Smith", jsonBillingAddress.getString("lastName"));
        assertEquals("1 Main St", jsonBillingAddress.getString("streetAddress"));
        assertEquals("Some Town", jsonBillingAddress.getString("locality"));
        assertEquals("12345", jsonBillingAddress.getString("postalCode"));
        assertEquals("Some Region", jsonBillingAddress.getString("region"));
        assertEquals("Some Country", jsonBillingAddress.getString("countryName"));

        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("form", jsonMetadata.getString("source"));
    }

    public void testBuildsWithAnExpirationDateCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber(VISA)
                .cvv("123")
                .expirationDate("01/15")
                .postalCode("12345");

        JSONObject jsonCard = new JSONObject(cardBuilder.toJsonString()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("01/15", jsonCard.getString("expirationDate"));
        assertEquals("12345", jsonCard.getJSONObject("billingAddress").getString("postalCode"));
    }

    public void testBuildsNestedAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        JSONObject billingAddress = new JSONObject(cardBuilder.toJsonString())
                .getJSONObject(CREDIT_CARD_KEY).getJSONObject(BILLING_ADDRESS_KEY);

        assertFalse(billingAddress.has("firstName"));
        assertFalse(billingAddress.has("lastName"));
        assertFalse(billingAddress.has("streetAddress"));
        assertFalse(billingAddress.has("locality"));
        assertEquals("60606", billingAddress.getString("postalCode"));
        assertFalse(billingAddress.has("region"));
        assertFalse(billingAddress.has("countryName"));
    }

    public void testUsesDefaultInfoForMetadata() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject metadata = new JSONObject(cardBuilder.toJsonString()).getJSONObject(Builder.METADATA_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    public void testSetsDefaultCardSource() {
        Card card = new CardBuilder().build();

        assertEquals("form", card.getSource());
    }

    public void testSetsCardSource() {
        Card card = new CardBuilder().source(VenmoAppSwitch.VENMO_SOURCE).build();

        assertEquals(VenmoAppSwitch.VENMO_SOURCE, card.getSource());
    }

    public void testSetsIntegrationMethod() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().integration("test-integration");

        JSONObject metadata = new JSONObject(cardBuilder.toJsonString()).getJSONObject(Builder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .validate(true);

        JSONObject builtCard = new JSONObject(cardBuilder.toJsonString()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(true, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    public void testDoesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject builtCard = new JSONObject(cardBuilder.toJsonString()).getJSONObject(CREDIT_CARD_KEY);

        assertFalse(builtCard.keys().hasNext());
    }

}
