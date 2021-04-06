package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class BraintreeNonceTest {

    @Test
    public void constructor_parsesGraphQLCardTokenizeJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesRestCardTokenizeJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE), new JSONObject(nonce.getJson()), true);
    }
}