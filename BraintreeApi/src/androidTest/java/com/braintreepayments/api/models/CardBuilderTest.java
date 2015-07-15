package com.braintreepayments.api.models;

import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.Venmo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.braintreepayments.testutils.CardNumber.VISA;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(JUnit4.class)
public class CardBuilderTest {

    private static final String CREDIT_CARD_KEY = "creditCard";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";

    @Test(timeout = 1000)
    @SmallTest
    public void build_correctlyBuildsACard() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber(VISA)
                .cvv("123")
                .expirationMonth("01")
                .expirationYear("2015")
                .firstName("Joe")
                .lastName("Smith")
                .streetAddress("1 Main St")
                .locality("Some Town")
                .postalCode("12345")
                .region("Some Region")
                .countryName("Some Country");

        JSONObject json = new JSONObject(cardBuilder.build());
        JSONObject jsonCard = json.getJSONObject(CREDIT_CARD_KEY);
        JSONObject jsonBillingAddress = jsonCard.getJSONObject(BILLING_ADDRESS_KEY);
        JSONObject jsonMetadata = json.getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertEquals(VISA, jsonCard.getString("number"));
        assertEquals("123", jsonCard.getString("cvv"));
        assertEquals("01", jsonCard.getString("expirationMonth"));
        assertEquals("2015", jsonCard.getString("expirationYear"));

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

    @Test(timeout = 1000)
    @SmallTest
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

    @Test(timeout = 1000)
    @SmallTest
    public void nestsAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        JSONObject billingAddress = new JSONObject(cardBuilder.build())
                .getJSONObject(CREDIT_CARD_KEY).getJSONObject(BILLING_ADDRESS_KEY);

        assertFalse(billingAddress.has("firstName"));
        assertFalse(billingAddress.has("lastName"));
        assertFalse(billingAddress.has("streetAddress"));
        assertFalse(billingAddress.has("locality"));
        assertEquals("60606", billingAddress.getString("postalCode"));
        assertFalse(billingAddress.has("region"));
        assertFalse(billingAddress.has("countryName"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void usesDefaultInfoForMetadata() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("form", metadata.getString("source"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void usesDefaultCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder();
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals("form", jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setsCardSource() throws JSONException {
        CardBuilder builder = new CardBuilder().source(Venmo.VENMO_SOURCE);
        JSONObject jsonObject = new JSONObject(builder.build());

        assertEquals(Venmo.VENMO_SOURCE, jsonObject.getJSONObject("_meta").getString("source"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setsIntegrationMethod() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().integration("test-integration");

        JSONObject metadata = new JSONObject(cardBuilder.build())
                .getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void includesValidateOptionWhenSet() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().validate(true);

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertEquals(true, builtCard.getJSONObject("options").getBoolean("validate"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void doesNotIncludeEmptyCreditCardWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject builtCard = new JSONObject(cardBuilder.build()).getJSONObject(CREDIT_CARD_KEY);

        assertFalse(builtCard.keys().hasNext());
    }
}
