package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UnionPayConfigurationUnitTest {

    private JSONObject mUnionPayConfiguration;

    @Before
    public void setup() throws JSONException {
        JSONObject configuration = new JSONObject(Fixtures.CONFIGURATION_WITH_UNIONPAY);
        mUnionPayConfiguration = configuration.getJSONObject("unionPay");
    }

    @Test
    public void fromJson_parsesWithUnionPayConfiguration() {
        UnionPayConfiguration unionPayConfiguration = UnionPayConfiguration.fromJson(mUnionPayConfiguration);

        assertTrue(unionPayConfiguration.isEnabled());
    }

    @Test
    public void fromJson_parsesEmptyUnionPayConfigurationIfNotDefined() {
        mUnionPayConfiguration = new JSONObject();
        UnionPayConfiguration unionPayConfiguration = UnionPayConfiguration.fromJson(mUnionPayConfiguration);

        assertFalse(unionPayConfiguration.isEnabled());
    }

    @Test
    public void fromJson_unionPayEnableIsFalseWhenFalse() throws JSONException {
        mUnionPayConfiguration.put("enabled", false);
        UnionPayConfiguration unionPayConfiguration = UnionPayConfiguration.fromJson(mUnionPayConfiguration);

        assertFalse(unionPayConfiguration.isEnabled());
    }

    @Test
    public void reportsUnionPayEnabledWhenEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_UNIONPAY);

        assertTrue(configuration.getUnionPay().isEnabled());
    }
}
