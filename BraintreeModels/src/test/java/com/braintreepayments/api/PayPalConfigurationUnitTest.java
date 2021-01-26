package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalConfigurationUnitTest {

    @Test
    public void parsesPayPalConfigurationFromToken() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_OFFLINE_PAYPAL);
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertTrue(configuration.isPayPalEnabled());
        assertTrue(payPalConfiguration.isEnabled());
        assertEquals("paypal_merchant", payPalConfiguration.getDisplayName());
        assertEquals("paypal_client_id", payPalConfiguration.getClientId());
        assertEquals("http://www.example.com/privacy", payPalConfiguration.getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", payPalConfiguration.getUserAgreementUrl());
        assertEquals("http://localhost:9000/v1/", payPalConfiguration.getDirectBaseUrl());
        assertEquals("offline", payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }

    @Test
    public void parsesPayPalConfigurationFromTokenForLive() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertTrue(configuration.isPayPalEnabled());
        assertTrue(payPalConfiguration.isEnabled());
        assertEquals("paypal_merchant", payPalConfiguration.getDisplayName());
        assertEquals("paypal_client_id", payPalConfiguration.getClientId());
        assertEquals("http://www.example.com/privacy", payPalConfiguration.getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", payPalConfiguration.getUserAgreementUrl());
        assertEquals("https://www.paypal.com/v1/", payPalConfiguration.getDirectBaseUrl());
        assertEquals("live", payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }

    @Test
    public void parsesPayPalConfigurationFromTokenForCustom() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CUSTOM_PAYPAL);
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertTrue(configuration.isPayPalEnabled());
        assertTrue(payPalConfiguration.isEnabled());
        assertEquals("paypal_merchant", payPalConfiguration.getDisplayName());
        assertEquals("paypal_client_id", payPalConfiguration.getClientId());
        assertEquals("http://www.example.com/privacy", payPalConfiguration.getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", payPalConfiguration.getUserAgreementUrl());
        assertEquals("https://braintree.paypal.com/v1/", payPalConfiguration.getDirectBaseUrl());
        assertEquals("custom", payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }

    @Test
    public void reportsPayPalNotEnabledWhenFlagged() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test
    public void reportsPayPalNotEnabledWhenConfigAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test
    public void exposesPayPalTouchKillSwitch() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAYPAL_TOUCH_DISABLED);

        assertTrue(configuration.getPayPal().isTouchDisabled());
    }

    @Test
    public void reportsPayPalEnabledWhenClientIdAbsentInOfflineMode() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_OFFLINE_PAYPAL_NO_CLIENT_ID);
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertTrue(payPalConfiguration.isEnabled());
    }

    @Test
    public void reportsPayPalNotEnabledWhenClientIdAbsentInLiveMode() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NO_CLIENT_ID);
        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertFalse(payPalConfiguration.isEnabled());
    }

    @Test
    public void fromJson_parsesConfiguration() throws JSONException {
        JSONObject json = new JSONObject(Fixtures.CONFIGURATION_WITH_OFFLINE_PAYPAL)
                .getJSONObject("paypal");

        PayPalConfiguration payPalConfiguration = PayPalConfiguration.fromJson(json);

        assertTrue(payPalConfiguration.isEnabled());
        assertEquals("paypal_merchant", payPalConfiguration.getDisplayName());
        assertEquals("paypal_client_id", payPalConfiguration.getClientId());
        assertEquals("http://www.example.com/privacy", payPalConfiguration.getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", payPalConfiguration.getUserAgreementUrl());
        assertEquals("http://localhost:9000/v1/", payPalConfiguration.getDirectBaseUrl());
        assertEquals("offline", payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }

    @Test
    public void fromJson_returnsNewPayPalConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        PayPalConfiguration payPalConfiguration = PayPalConfiguration.fromJson(null);

        assertFalse(payPalConfiguration.isEnabled());
        assertNull(payPalConfiguration.getDisplayName());
        assertNull(payPalConfiguration.getClientId());
        assertNull(payPalConfiguration.getPrivacyUrl());
        assertNull(payPalConfiguration.getUserAgreementUrl());
        assertNull(payPalConfiguration.getDirectBaseUrl());
        assertNull(payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }

    @Test
    public void fromJson_returnsNewPayPalConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        PayPalConfiguration payPalConfiguration = PayPalConfiguration.fromJson(new JSONObject());

        assertFalse(payPalConfiguration.isEnabled());
        assertNull(payPalConfiguration.getDisplayName());
        assertNull(payPalConfiguration.getClientId());
        assertNull(payPalConfiguration.getPrivacyUrl());
        assertNull(payPalConfiguration.getUserAgreementUrl());
        assertNull(payPalConfiguration.getDirectBaseUrl());
        assertNull(payPalConfiguration.getEnvironment());
        assertTrue(payPalConfiguration.isTouchDisabled());
    }
}
