package com.braintreepayments.api.models;

import com.braintreepayments.api.Utils;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class CardBuilderTest extends TestCase {

    public void testBuildsACardCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber("41111111111111111")
                .cvv("123")
                .expirationMonth("01")
                .expirationYear("2015")
                .postalCode("12345");

        Card card = cardBuilder.build();
        JSONObject builtCard = new JSONObject(Utils.getGson().toJson(card));

        assertEquals("41111111111111111", builtCard.getString("number"));
        assertEquals("123", builtCard.getString("cvv"));
        assertEquals("01", builtCard.getString("expirationMonth"));
        assertEquals("2015", builtCard.getString("expirationYear"));
        assertEquals("12345", builtCard.getJSONObject("billingAddress").getString("postalCode"));
    }

    public void testBuildsWithAnExpirationDateCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder().cardNumber("41111111111111111")
                .cvv("123")
                .expirationDate("01/15")
                .postalCode("12345");

        Card card = cardBuilder.build();
        JSONObject builtCard = new JSONObject(Utils.getGson().toJson(card));

        assertEquals("41111111111111111", builtCard.getString("number"));
        assertEquals("123", builtCard.getString("cvv"));
        assertEquals("01/15", builtCard.getString("expirationDate"));
        assertEquals("12345", builtCard.getJSONObject("billingAddress").getString("postalCode"));
    }

    public void testBuildsNestedAddressCorrectly() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .postalCode("60606");

        Card card = cardBuilder.build();
        JSONObject builtCard = new JSONObject(Utils.getGson().toJson(card));

        assertEquals("60606", builtCard.getJSONObject("billingAddress").getString("postalCode"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder()
                .validate(true);

        JSONObject jsonCard = new JSONObject(Utils.getGson().toJson(cardBuilder.build()));

        assertEquals(true, jsonCard.getJSONObject("options").getBoolean("validate"));
    }

    public void testDoesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        CardBuilder cardBuilder = new CardBuilder();

        JSONObject jsonCard = new JSONObject(Utils.getGson().toJson(cardBuilder.build()));

        assertFalse(jsonCard.keys().hasNext());
    }
}
