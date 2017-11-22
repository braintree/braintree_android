package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.test.VenmoInstalledContextFactory;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoConfigurationUnitTest {

    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(stringFromFixture("configuration/with_pay_with_venmo.json"));
    }

    @Test
    public void isVenmoWhitelisted_returnsTrueForWhitelist() {
        assertTrue(mConfiguration.getPayWithVenmo().isVenmoWhitelisted(VenmoInstalledContextFactory
                .venmoInstalledContext(true).getContentResolver()));
    }

    @Test
    public void fromJson_parsesPayWithVenmoConfiguration() throws JSONException {
        assertEquals("access-token", mConfiguration.getPayWithVenmo().getAccessToken());
        assertEquals("environment", mConfiguration.getPayWithVenmo().getEnvironment());
        assertEquals("merchant-id", mConfiguration.getPayWithVenmo().getMerchantId());
    }

    @Test
    public void fromJson_parsesEmptyVenmoConfigurationWhenConfigurationDoesntHavePayWithVenmo() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertEquals("", configuration.getPayWithVenmo().getAccessToken());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void isEnabled_returnsTrueWhenEnabled() throws JSONException {
        assertFalse(TextUtils.isEmpty(mConfiguration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void isEnabled_returnsTrueWhenAppIsInstalled() throws JSONException {
        disableSignatureVerification();
        assertTrue(mConfiguration.getPayWithVenmo().isEnabled(VenmoInstalledContextFactory.venmoInstalledContext(true)));
    }

    @Test
    public void isEnabled_returnsFalseForNotInstalled() {
        assertFalse(mConfiguration.getPayWithVenmo().isEnabled(VenmoInstalledContextFactory.venmoInstalledContext(false)));
    }
}
