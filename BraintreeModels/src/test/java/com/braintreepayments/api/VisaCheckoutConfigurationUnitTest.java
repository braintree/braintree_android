package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VisaCheckoutConfiguration.class, ClassHelper.class })
public class VisaCheckoutConfigurationUnitTest {

    @Test
    public void fromJson_parsesFullInput() throws JSONException {
        JSONObject input = new JSONObject()
                .put("apikey", "sample-api-key")
                .put("externalClientId", "sample-external-client-id")
                .put("supportedCardTypes", new JSONArray()
                        .put("American Express")
                        .put("Visa")
                        .put("Discover")
                        .put("MasterCard"));

        VisaCheckoutConfiguration sut = VisaCheckoutConfiguration.fromJson(input);
        assertTrue(sut.isEnabled());
        assertEquals("sample-api-key", sut.getApiKey());
        assertEquals("sample-external-client-id", sut.getExternalClientId());

        List<String> expectedCardBrands = Arrays.asList("AMEX", "VISA", "DISCOVER", "MASTERCARD");
        assertEquals(expectedCardBrands, sut.getAcceptedCardBrands());
    }

    @Test
    public void fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        VisaCheckoutConfiguration sut = VisaCheckoutConfiguration.fromJson(null);
        assertFalse(sut.isEnabled());
        assertEquals("", sut.getApiKey());
        assertEquals("", sut.getExternalClientId());
        assertTrue(sut.getAcceptedCardBrands().isEmpty());
    }

    @Test
    public void fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        VisaCheckoutConfiguration sut = VisaCheckoutConfiguration.fromJson(new JSONObject());
        assertFalse(sut.isEnabled());
        assertEquals("", sut.getApiKey());
        assertEquals("", sut.getExternalClientId());
        assertTrue(sut.getAcceptedCardBrands().isEmpty());
    }
}
