package com.braintreepayments.api.models;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class KountConfigurationUnitTest {

    @Test
    public void parsesKountConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration_with_kount.json"));
        KountConfiguration kountConfiguration = configuration.getKount();

        assertTrue(kountConfiguration.isEnabled());
        assertEquals("600000", kountConfiguration.getKountMerchantId());
    }
}
