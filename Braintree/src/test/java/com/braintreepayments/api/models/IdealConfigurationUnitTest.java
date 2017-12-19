package com.braintreepayments.api.models;

import com.braintreepayments.testutils.FixturesHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class IdealConfigurationUnitTest {

    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(FixturesHelper.stringFromFixture("configuration/with_ideal.json"));
    }

    @Test
    public void parsesRouteId() {
        assertEquals("route_nxyqkq_s654wq_92jr64_mnr4kr_yjz", mConfiguration.getIdealConfiguration().getRouteId());
    }

    @Test
    public void isEnabledReturnsTrueIfEnabled() {
        assertTrue(mConfiguration.getIdealConfiguration().isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNotEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(FixturesHelper.stringFromFixture("configuration/configuration.json"));
        assertFalse(configuration.getIdealConfiguration().isEnabled());
    }
}
