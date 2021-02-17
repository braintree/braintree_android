package com.braintreepayments.api.models;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.Configuration;
import com.braintreepayments.api.GooglePayConfiguration;
import com.braintreepayments.api.Fixtures;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4ClassRunner.class)
public class GooglePayConfigurationTest {

    @Test(timeout = 1000)
    public void parsesGooglePayConfigurationFromToken() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        GooglePayConfiguration googlePayConfiguration = configuration.getGooglePay();

        assertTrue(googlePayConfiguration.isEnabled());
        assertEquals("google-auth-fingerprint", googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Google Pay Merchant", googlePayConfiguration.getDisplayName());
        assertEquals("sandbox", googlePayConfiguration.getEnvironment());
        assertEquals("visa", googlePayConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", googlePayConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", googlePayConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", googlePayConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_parsesConfiguration() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        JSONObject json = new JSONObject(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
                .getJSONObject("androidPay");

        GooglePayConfiguration googlePayConfiguration = GooglePayConfiguration.fromJson(json);

        assertTrue(googlePayConfiguration.isEnabled());
        assertEquals("google-auth-fingerprint", googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Google Pay Merchant", googlePayConfiguration.getDisplayName());
        assertEquals("sandbox", googlePayConfiguration.getEnvironment());
        assertEquals("visa", googlePayConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", googlePayConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", googlePayConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", googlePayConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        GooglePayConfiguration googlePayConfiguration = GooglePayConfiguration.fromJson(null);

        assertFalse(googlePayConfiguration.isEnabled());
        assertNull(googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePayConfiguration.getDisplayName());
        assertNull(googlePayConfiguration.getEnvironment());
        assertEquals(0, googlePayConfiguration.getSupportedNetworks().length);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        GooglePayConfiguration googlePayConfiguration = GooglePayConfiguration.fromJson(new JSONObject());

        assertFalse(googlePayConfiguration.isEnabled());
        assertNull(googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePayConfiguration.getDisplayName());
        assertNull(googlePayConfiguration.getEnvironment());
        assertEquals(0, googlePayConfiguration.getSupportedNetworks().length);
    }

    private static boolean hasGooglePlayServices() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationProvider.getApplicationContext()) ==
                ConnectionResult.SUCCESS;
    }
}
