package com.braintreepayments.api.models;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.testutils.Fixtures;
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
public class GooglePaymentConfigurationTest {

    @Test(timeout = 1000)
    public void parsesGooglePaymentConfigurationFromToken() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANDROID_PAY);
        GooglePaymentConfiguration googlePaymentConfiguration = configuration.getGooglePayment();

        assertTrue(googlePaymentConfiguration.isEnabled(ApplicationProvider.getApplicationContext()));
        assertEquals("google-auth-fingerprint", googlePaymentConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Android Pay Merchant", googlePaymentConfiguration.getDisplayName());
        assertEquals("sandbox", googlePaymentConfiguration.getEnvironment());
        assertEquals("visa", googlePaymentConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", googlePaymentConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", googlePaymentConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", googlePaymentConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_parsesConfiguration() throws JSONException {
        assumeTrue("Not using a Google Play Services device", hasGooglePlayServices());

        JSONObject json = new JSONObject(Fixtures.CONFIGURATION_WITH_ANDROID_PAY)
                .getJSONObject("androidPay");

        GooglePaymentConfiguration googlePaymentConfiguration = GooglePaymentConfiguration.fromJson(json);

        assertTrue(googlePaymentConfiguration.isEnabled(ApplicationProvider.getApplicationContext()));
        assertEquals("google-auth-fingerprint", googlePaymentConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Android Pay Merchant", googlePaymentConfiguration.getDisplayName());
        assertEquals("sandbox", googlePaymentConfiguration.getEnvironment());
        assertEquals("visa", googlePaymentConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", googlePaymentConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", googlePaymentConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", googlePaymentConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePaymentConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        GooglePaymentConfiguration googlePaymentConfiguration = GooglePaymentConfiguration.fromJson(null);

        assertFalse(googlePaymentConfiguration.isEnabled(ApplicationProvider.getApplicationContext()));
        assertNull(googlePaymentConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePaymentConfiguration.getDisplayName());
        assertNull(googlePaymentConfiguration.getEnvironment());
        assertEquals(0, googlePaymentConfiguration.getSupportedNetworks().length);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewGooglePaymentConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        GooglePaymentConfiguration googlePaymentConfiguration = GooglePaymentConfiguration.fromJson(new JSONObject());

        assertFalse(googlePaymentConfiguration.isEnabled(ApplicationProvider.getApplicationContext()));
        assertNull(googlePaymentConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", googlePaymentConfiguration.getDisplayName());
        assertNull(googlePaymentConfiguration.getEnvironment());
        assertEquals(0, googlePaymentConfiguration.getSupportedNetworks().length);
    }

    private static boolean hasGooglePlayServices() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ApplicationProvider.getApplicationContext()) ==
                ConnectionResult.SUCCESS;
    }
}
