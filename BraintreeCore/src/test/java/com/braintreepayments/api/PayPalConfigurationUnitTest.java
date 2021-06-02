package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalConfigurationUnitTest {

    @Test
    public void fromJson_parsesFullInput() throws JSONException {
        JSONObject input = new JSONObject()
                .put("displayName", "sample display name")
                .put("clientId", "sample-client-id")
                .put("privacyUrl", "https://example.com/privacy_url")
                .put("userAgreementUrl", "https://example.com/user_agreement_url")
                .put("directBaseUrl", "https://example.com/direct_base_url")
                .put("environment", "live")
                .put("touchDisabled", true)
                .put("currencyIsoCode", "USD");

        PayPalConfiguration sut = PayPalConfiguration.fromJson(input);
        assertEquals("sample display name", sut.getDisplayName());
        assertEquals("sample-client-id", sut.getClientId());
        assertEquals("https://example.com/privacy_url", sut.getPrivacyUrl());
        assertEquals("https://example.com/user_agreement_url", sut.getUserAgreementUrl());
        assertEquals("https://example.com/direct_base_url/v1/", sut.getDirectBaseUrl());
        assertEquals("live", sut.getEnvironment());
        assertEquals("USD", sut.getCurrencyIsoCode());
        assertTrue(sut.isTouchDisabled());
    }

    @Test
    public void fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        PayPalConfiguration sut = PayPalConfiguration.fromJson(null);
        assertEquals("", sut.getDisplayName());
        assertEquals("", sut.getClientId());
        assertEquals("", sut.getPrivacyUrl());
        assertEquals("", sut.getUserAgreementUrl());
        assertEquals("", sut.getDirectBaseUrl());
        assertEquals("", sut.getEnvironment());
        assertTrue(sut.isTouchDisabled());
    }

    @Test
    public void fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        PayPalConfiguration sut = PayPalConfiguration.fromJson(new JSONObject());
        assertEquals("", sut.getDisplayName());
        assertEquals("", sut.getClientId());
        assertEquals("", sut.getPrivacyUrl());
        assertEquals("", sut.getUserAgreementUrl());
        assertEquals("", sut.getDirectBaseUrl());
        assertEquals("", sut.getEnvironment());
        assertTrue(sut.isTouchDisabled());
    }
}
