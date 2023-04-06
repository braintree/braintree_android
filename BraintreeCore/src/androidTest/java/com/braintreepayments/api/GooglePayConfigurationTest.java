package com.braintreepayments.api;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class GooglePayConfigurationTest {

    @Test(timeout = 1000)
    public void parsesGooglePayConfigurationFromToken() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);

        assertTrue(configuration.isGooglePayEnabled());
        assertEquals("google-auth-fingerprint", configuration.getGooglePayAuthorizationFingerprint());
        assertEquals("Google Pay Merchant", configuration.getGooglePayDisplayName());
        assertEquals("sandbox", configuration.getGooglePayEnvironment());
        assertEquals("visa", configuration.getGooglePaySupportedNetworks().get(0));
        assertEquals("mastercard", configuration.getGooglePaySupportedNetworks().get(1));
        assertEquals("amex", configuration.getGooglePaySupportedNetworks().get(2));
        assertEquals("discover", configuration.getGooglePaySupportedNetworks().get(3));
    }

    @Test(timeout = 1000)
    public void fromJson_parsesConfiguration() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        JSONObject json = new JSONObject(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
                .getJSONObject("androidPay");

        GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration(json);

        assertTrue(googlePayConfiguration.isEnabled());
        assertEquals("google-auth-fingerprint", googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Google Pay Merchant", googlePayConfiguration.getDisplayName());
        assertEquals("sandbox", googlePayConfiguration.getEnvironment());
        assertEquals("visa", googlePayConfiguration.getSupportedNetworks().get(0));
        assertEquals("mastercard", googlePayConfiguration.getSupportedNetworks().get(1));
        assertEquals("amex", googlePayConfiguration.getSupportedNetworks().get(2));
        assertEquals("discover", googlePayConfiguration.getSupportedNetworks().get(3));
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration(null);

        assertFalse(googlePayConfiguration.isEnabled());
        assertNull(googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePayConfiguration.getDisplayName());
        assertNull(googlePayConfiguration.getEnvironment());
        assertEquals(0, googlePayConfiguration.getSupportedNetworks().size());
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        GooglePayConfiguration googlePayConfiguration = new GooglePayConfiguration(new JSONObject());

        assertFalse(googlePayConfiguration.isEnabled());
        assertNull(googlePayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePayConfiguration.getDisplayName());
        assertNull(googlePayConfiguration.getEnvironment());
        assertEquals(0, googlePayConfiguration.getSupportedNetworks().size());
    }

    private static boolean hasGooglePlayServices() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationProvider.getApplicationContext()) ==
                ConnectionResult.SUCCESS;
    }
}
