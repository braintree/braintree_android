package com.braintreepayments.api.models;

import com.braintreepayments.api.models.PaymentMethod.Builder;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class CoinbaseAccountBuilderTest extends TestCase {

    private static final String COINBASE_KEY = "coinbaseAccount";

    public void testBuildsACoinbaseAccountCorrectly() throws JSONException {
        CoinbaseAccountBuilder coinbaseAccountBuilder = new CoinbaseAccountBuilder()
                .code("coinbase-code");

        JSONObject json = new JSONObject(coinbaseAccountBuilder.toJsonString());
        JSONObject jsonAccount = json.getJSONObject(COINBASE_KEY);
        JSONObject jsonMetadata = json.getJSONObject(Builder.METADATA_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("coinbase-code", jsonAccount.getString("code"));
        assertEquals("custom", jsonMetadata.getString("integration"));
    }

    public void testUsesCorrectInfoForMetadata() throws JSONException {
        CoinbaseAccountBuilder coinbaseAccountBuilder = new CoinbaseAccountBuilder()
                .source("browser");

        JSONObject metadata = new JSONObject(coinbaseAccountBuilder.toJsonString())
                .getJSONObject(Builder.METADATA_KEY);

        assertEquals("custom", metadata.get("integration"));
        assertEquals("browser", metadata.getString("source"));
    }

    public void testSetsIntegrationMethod() throws JSONException {
        CoinbaseAccountBuilder coinbaseAccountBuilder = new CoinbaseAccountBuilder()
                .integration("test-integration");

        JSONObject metadata = new JSONObject(coinbaseAccountBuilder.toJsonString())
                .getJSONObject(Builder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        CoinbaseAccountBuilder coinbaseAccountBuilder = new CoinbaseAccountBuilder()
                .validate(true);

        JSONObject coinbaseAccount = new JSONObject(coinbaseAccountBuilder.toJsonString())
                .getJSONObject(COINBASE_KEY);

        assertEquals(true, coinbaseAccount.getJSONObject("options").getBoolean("validate"));
    }

    public void testDoesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        CoinbaseAccountBuilder coinbaseAccountBuilder = new CoinbaseAccountBuilder();

        JSONObject coinbaseAccount = new JSONObject(coinbaseAccountBuilder.toJsonString())
                .getJSONObject(COINBASE_KEY);

        assertFalse(coinbaseAccount.keys().hasNext());
    }
}
