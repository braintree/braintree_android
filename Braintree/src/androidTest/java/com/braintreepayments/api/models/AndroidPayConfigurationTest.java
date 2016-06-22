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
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_android_pay.json"));

        failIfNotGooglePlayServicesDevice();

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
        JSONObject json = new JSONObject(stringFromFixture("configuration_with_android_pay.json"))
                .getJSONObject("androidPay");

        failIfNotGooglePlayServicesDevice();

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

        failIfNotGooglePlayServicesDevice();

        assertFalse(androidPayConfiguration.isEnabled(getTargetContext()));
        assertNull(androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertNull(androidPayConfiguration.getDisplayName());
        assertNull(androidPayConfiguration.getEnvironment());
        assertEquals(0, androidPayConfiguration.getSupportedNetworks().length);
    }

    @Test(timeout = 1000)
    public void fromJson_returnsNewAndroidPayConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.fromJson(new JSONObject());

        failIfNotGooglePlayServicesDevice();

        assertFalse(androidPayConfiguration.isEnabled(getTargetContext()));
        assertNull(androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertNull(androidPayConfiguration.getDisplayName());
        assertNull(androidPayConfiguration.getEnvironment());
        assertEquals(0, androidPayConfiguration.getSupportedNetworks().length);
    }

    public static void failIfNotGooglePlayServicesDevice(){
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getTargetContext()) !=
                ConnectionResult.SUCCESS){
            fail("Not using a Google Play Services device.");
        }
    }
}
