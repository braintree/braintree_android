package com.braintreepayments.api.models;

import android.content.Context;
import android.text.TextUtils;

import com.braintreepayments.api.test.VenmoMockContext;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.api.internal.SignatureVerificationUnitTestUtils.disableSignatureVerification;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class VenmoConfigurationTest {

    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(stringFromFixture("configuration_with_pay_with_venmo.json"));
    }

    @Test
    public void fromJson_parsesPayWithVenmoConfiguration() throws JSONException {
        assertEquals("access-token", mConfiguration.getPayWithVenmo().getAccessToken());
        assertEquals("environment", mConfiguration.getPayWithVenmo().getEnvironment());
        assertEquals("merchant-id", mConfiguration.getPayWithVenmo().getMerchantId());
    }

    @Test
    public void fromJson_parsesEmptyVenmoConfigurationWhenConfigurationDoesntHavePayWithVenmo() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration.json"));

        assertEquals("", configuration.getPayWithVenmo().getAccessToken());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void isEnabled_returnsTrueWhenEnabled() throws JSONException {
        assertFalse(TextUtils.isEmpty(mConfiguration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void isVenmoWhitelisted_returnsTrueForWhitelist() throws JSONException {
        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .build();

        assertTrue(mConfiguration.getPayWithVenmo().isVenmoWhitelisted(context.getContentResolver()));
    }

    @Test
    public void isVenmoWhitelisted_returnsFalseForInvalidWhitelist() {
        Context context = new VenmoMockContext()
                .whitelistValue("false")
                .build();

        assertFalse(mConfiguration.getPayWithVenmo().isVenmoWhitelisted(context.getContentResolver()));
    }

    @Test
    public void isVenmoWhitelisted_returnsFalseForJunkContentProviderAndExceptionIsPosted() {
        Context context = new VenmoMockContext()
                .whitelistValue("neither")
                .build();

        assertFalse(mConfiguration.getPayWithVenmo().isVenmoWhitelisted(context.getContentResolver()));
    }

    @Test
    public void isEnabled_returnsTrueWhenAppIsInstalled() throws JSONException {
        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .venmoInstalled()
                .build();

        disableSignatureVerification();

        assertTrue(mConfiguration.getPayWithVenmo().isEnabled(context));
    }

    @Test
    public void isEnabled_returnsFalseForNotInstalled() {
        Context context = new VenmoMockContext()
                .whitelistValue("true")
                .build();

        assertFalse(mConfiguration.getPayWithVenmo().isEnabled(context));
    }
}
