package com.braintreepayments.api.models;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class KountConfigurationUnitTest {

    @Test
    public void parsesKountConfigurationAsAlwaysDisabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_kount.json"));
        KountConfiguration kountConfiguration = configuration.getKount();

        assertFalse(kountConfiguration.isEnabled());
        assertEquals("", kountConfiguration.getKountMerchantId());
    }
}
