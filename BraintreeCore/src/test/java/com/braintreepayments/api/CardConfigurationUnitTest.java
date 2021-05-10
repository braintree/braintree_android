package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardConfigurationUnitTest {

    @Test
    public void fromJson_parsesFullInput() throws JSONException {
        JSONObject input = new JSONObject()
                .put("collectDeviceData", true)
                .put("supportedCardTypes", new JSONArray()
                        .put("American Express")
                        .put("Discover")
                        .put("JCB")
                        .put("MasterCard")
                        .put("Visa"));
        CardConfiguration sut = CardConfiguration.fromJson(input);

        assertTrue(sut.isFraudDataCollectionEnabled());
        assertEquals(5, sut.getSupportedCardTypes().size());
        assertEquals("American Express", sut.getSupportedCardTypes().get(0));
        assertEquals("Discover", sut.getSupportedCardTypes().get(1));
        assertEquals("JCB", sut.getSupportedCardTypes().get(2));
        assertEquals("MasterCard", sut.getSupportedCardTypes().get(3));
        assertEquals("Visa", sut.getSupportedCardTypes().get(4));
    }

    @Test
    public void fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        CardConfiguration sut = CardConfiguration.fromJson(null);
        assertFalse(sut.isFraudDataCollectionEnabled());
        assertEquals(0, sut.getSupportedCardTypes().size());
    }

    @Test
    public void fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        CardConfiguration sut = CardConfiguration.fromJson(new JSONObject());
        assertFalse(sut.isFraudDataCollectionEnabled());
        assertEquals(0, sut.getSupportedCardTypes().size());
    }
}
