package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import com.braintreepayments.api.internal.SignatureVerificationTestUtils;
import com.braintreepayments.testutils.MockContextForVenmo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VenmoConfigurationTest {

    private Configuration configurationWithVenmo;

    @Before
    public void setup() throws JSONException {
        configurationWithVenmo = Configuration.fromJson(
                stringFromFixture("configuration_with_pay_with_venmo.json"));
    }

    @Test(timeout = 1000)
    public void fromJson_parsesPayWithVenmoConfiguration() throws JSONException {
        assertEquals("access-token", configurationWithVenmo.getPayWithVenmo().getAccessToken());
    }

    @Test(timeout = 1000)
    public void fromJson_parsesEmptyVenmoConfigurationWhenConfigurationDoesntHavePayWithVenmo()
            throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration.json"));

        VenmoConfiguration venmoConfiguration = configuration.getPayWithVenmo();
        assertEquals("", venmoConfiguration.getAccessToken());
        assertTrue(TextUtils.isEmpty(venmoConfiguration.getAccessToken()));
    }

    @Test(timeout = 1000)
    public void isEnabled_returnsTrueWhenEnabled() throws JSONException {
        VenmoConfiguration venmoConfiguration = configurationWithVenmo.getPayWithVenmo();
        assertFalse(TextUtils.isEmpty(venmoConfiguration.getAccessToken()));
    }

    @Test(timeout = 1000)
    public void isVenmoWhitelisted_returnsTrueForWhitelist() throws JSONException {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("true")
                .build();

        assertTrue(configurationWithVenmo.getPayWithVenmo()
                .isVenmoWhitelisted(mockContext.getContentResolver()));
    }

    @Test(timeout = 1000)
    public void isVenmoWhitelisted_returnsFalseForInvalidWhitelist() {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("false")
                .build();

        assertFalse(configurationWithVenmo.getPayWithVenmo()
                .isVenmoWhitelisted(mockContext.getContentResolver()));
    }

    @Test(timeout = 1000)
    public void isVenmoWhitelisted_returnsFalseForJunkContentProviderAndExceptionIsPosted() {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("neither")
                .build();

        assertFalse(configurationWithVenmo.getPayWithVenmo()
                .isVenmoWhitelisted(mockContext.getContentResolver()));
    }

    @Test(timeout = 1000)
    public void isEnabled_returnsTrueWhenAppIsInstalled() throws JSONException {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("true")
                .venmoInstalled()
                .build();

        SignatureVerificationTestUtils.disableSignatureVerification();

        assertTrue(configurationWithVenmo.getPayWithVenmo().isEnabled(mockContext));
    }

    @Test(timeout = 1000)
    public void isEnabled_returnsFalseForNotInstalled() {
        MockContext mockContext = new MockContextForVenmo()
                .whitelistValue("true")
                .build();

        SignatureVerificationTestUtils.disableSignatureVerification();

        assertFalse(configurationWithVenmo.getPayWithVenmo().isEnabled(mockContext));
    }
}
