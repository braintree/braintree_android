package com.braintreepayments.api;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class KountConfigurationUnitTest {

    @Test
    public void parsesKountConfiguration() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT);
        KountConfiguration kountConfiguration = configuration.getKount();

        assertTrue(kountConfiguration.isEnabled());
        assertEquals("600000", kountConfiguration.getKountMerchantId());
    }
}
