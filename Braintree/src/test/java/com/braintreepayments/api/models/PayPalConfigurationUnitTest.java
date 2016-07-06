package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class PayPalConfigurationUnitTest {

    @Test
    public void parsesPayPalConfigurationFromToken() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_offline_paypal.json"));
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
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration_with_live_paypal.json"));
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
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_custom_paypal.json"));
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
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_disabled_paypal.json"));

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test
    public void reportsPayPalNotEnabledWhenConfigAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_disabled_paypal.json"));

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test
    public void exposesPayPalTouchKillSwitch() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_paypal_touch_disabled.json"));

        assertTrue(configuration.getPayPal().isTouchDisabled());
    }

    @Test
    public void fromJson_parsesConfiguration() throws JSONException {
        JSONObject json = new JSONObject(stringFromFixture("configuration_with_offline_paypal.json"))
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
