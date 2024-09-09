package com.braintreepayments.api.core

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class GooglePayConfigurationTest {

    @Test(timeout = 1000)
    @Throws(JSONException::class)
    fun parsesGooglePayConfigurationFromToken() {
        Assume.assumeTrue("Not using a Google Play Services device", hasGooglePlayServices())

        val configuration = fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)

        assertTrue(configuration.isGooglePayEnabled)
        assertEquals("google-auth-fingerprint", configuration.googlePayAuthorizationFingerprint)
        assertEquals("Google Pay Merchant", configuration.googlePayDisplayName)
        assertEquals("sandbox", configuration.googlePayEnvironment)
        assertEquals("visa", configuration.googlePaySupportedNetworks[0])
        assertEquals("mastercard", configuration.googlePaySupportedNetworks[1])
        assertEquals("amex", configuration.googlePaySupportedNetworks[2])
        assertEquals("discover", configuration.googlePaySupportedNetworks[3])
    }

    @Test(timeout = 1000)
    @Throws(JSONException::class)
    fun fromJson_parsesConfiguration() {
        Assume.assumeTrue("Not using a Google Play Services device", hasGooglePlayServices())

        val json = JSONObject(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
            .getJSONObject("androidPay")

        val googlePayConfiguration = GooglePayConfiguration(json)

        assertTrue(googlePayConfiguration.isEnabled)
        assertEquals(
            "google-auth-fingerprint",
            googlePayConfiguration.googleAuthorizationFingerprint
        )
        assertEquals("Google Pay Merchant", googlePayConfiguration.displayName)
        assertEquals("sandbox", googlePayConfiguration.environment)
        assertEquals("visa", googlePayConfiguration.supportedNetworks[0])
        assertEquals("mastercard", googlePayConfiguration.supportedNetworks[1])
        assertEquals("amex", googlePayConfiguration.supportedNetworks[2])
        assertEquals("discover", googlePayConfiguration.supportedNetworks[3])
    }

    @Test(timeout = 1000)
    fun fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenJSONObjectIsNull() {
        val googlePayConfiguration = GooglePayConfiguration(null)

        assertFalse(googlePayConfiguration.isEnabled)
        assertNull(googlePayConfiguration.googleAuthorizationFingerprint)
        assertEquals("", googlePayConfiguration.displayName)
        assertNull(googlePayConfiguration.environment)
        assertEquals(0, googlePayConfiguration.supportedNetworks.size.toLong())
    }

    @Test(timeout = 1000)
    fun fromJson_returnsNewGooglePayConfigurationWithDefaultValuesWhenNoDataIsPresent() {
        val googlePayConfiguration = GooglePayConfiguration(JSONObject())

        assertFalse(googlePayConfiguration.isEnabled)
        assertNull(googlePayConfiguration.googleAuthorizationFingerprint)
        assertEquals("", googlePayConfiguration.displayName)
        assertNull(googlePayConfiguration.environment)
        assertEquals(0, googlePayConfiguration.supportedNetworks.size.toLong())
    }

    companion object {
        private fun hasGooglePlayServices(): Boolean {
            return GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(ApplicationProvider.getApplicationContext()) ==
                    ConnectionResult.SUCCESS
        }
    }
}
