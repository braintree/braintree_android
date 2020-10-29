package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoConfigurationUnitTest {

    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
    }

    @Test
    public void fromJson_parsesPayWithVenmoConfiguration() {
        assertEquals("access-token", mConfiguration.getPayWithVenmo().getAccessToken());
        assertEquals("environment", mConfiguration.getPayWithVenmo().getEnvironment());
        assertEquals("merchant-id", mConfiguration.getPayWithVenmo().getMerchantId());
    }

    @Test
    public void fromJson_parsesEmptyVenmoConfigurationWhenConfigurationDoesntHavePayWithVenmo() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertEquals("", configuration.getPayWithVenmo().getAccessToken());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }
}
