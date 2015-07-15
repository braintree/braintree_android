package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PayPalConfigurationTest {

    @Test(timeout = 1000)
    @SmallTest
    public void parsesPayPalConfigurationFromToken() throws JSONException {
        Configuration configuration = getConfiguration("configuration_with_offline_paypal.json");

        assertTrue(configuration.isPayPalEnabled());

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

    @Test(timeout = 1000)
    @SmallTest
    public void reportsPayPalNotEnabledWhenFlagged() throws JSONException {
        Configuration configuration = getConfiguration("configuration_with_disabled_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void reportsPayPalNotEnabledWhenConfigAbsent() throws JSONException {
        Configuration configuration = getConfiguration("configuration_with_disabled_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
        assertFalse(configuration.getPayPal().isEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void exposesPayPalTouchKillSwitch() throws JSONException {
        Configuration configuration = getConfiguration("configuration_with_paypal_touch_disabled.json");

        assertTrue(configuration.getPayPal().isTouchDisabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesConfiguration() throws JSONException {
        JSONObject json = new JSONObject(stringFromFixture(getTargetContext(), "configuration_with_offline_paypal.json"))
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

    @Test(timeout = 1000)
    @SmallTest
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

    @Test(timeout = 1000)
    @SmallTest
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

    /* helpers */
    private Configuration getConfiguration(String fixture) throws JSONException {
        return Configuration.fromJson(stringFromFixture(getTargetContext(), fixture));
    }
}
