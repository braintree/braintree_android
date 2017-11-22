package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class AndroidPayConfigurationTest {

    @Test(timeout = 1000)
    public void parsesAndroidPayConfigurationFromToken() throws JSONException {
        failIfNotGooglePlayServicesDevice();

        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_android_pay.json"));

        AndroidPayConfiguration androidPayConfiguration = configuration.getAndroidPay();

        assertTrue(androidPayConfiguration.isEnabled(getTargetContext()));
        assertEquals("google-auth-fingerprint", androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Android Pay Merchant", androidPayConfiguration.getDisplayName());
        assertEquals("sandbox", androidPayConfiguration.getEnvironment());
        assertEquals("visa", androidPayConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", androidPayConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", androidPayConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", androidPayConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_parsesConfiguration() throws JSONException {
        failIfNotGooglePlayServicesDevice();

        JSONObject json = new JSONObject(stringFromFixture("configuration/with_android_pay.json"))
                .getJSONObject("androidPay");

        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.fromJson(json);

        assertTrue(androidPayConfiguration.isEnabled(getTargetContext()));
        assertEquals("google-auth-fingerprint", androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Android Pay Merchant", androidPayConfiguration.getDisplayName());
        assertEquals("sandbox", androidPayConfiguration.getEnvironment());
        assertEquals("visa", androidPayConfiguration.getSupportedNetworks()[0]);
        assertEquals("mastercard", androidPayConfiguration.getSupportedNetworks()[1]);
        assertEquals("amex", androidPayConfiguration.getSupportedNetworks()[2]);
        assertEquals("discover", androidPayConfiguration.getSupportedNetworks()[3]);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewAndroidPayConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.fromJson(null);

        assertFalse(androidPayConfiguration.isEnabled(getTargetContext()));
        assertNull(androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", androidPayConfiguration.getDisplayName());
        assertNull(androidPayConfiguration.getEnvironment());
        assertEquals(0, androidPayConfiguration.getSupportedNetworks().length);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewAndroidPayConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.fromJson(new JSONObject());

        assertFalse(androidPayConfiguration.isEnabled(getTargetContext()));
        assertNull(androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("", androidPayConfiguration.getDisplayName());
        assertNull(androidPayConfiguration.getEnvironment());
        assertEquals(0, androidPayConfiguration.getSupportedNetworks().length);
    }

    private static void failIfNotGooglePlayServicesDevice(){
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getTargetContext()) !=
                ConnectionResult.SUCCESS) {
            fail("Not using a Google Play Services device.");
        }
    }
}
