package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class UnionPayConfigurationUnitTest {

    private JSONObject mUnionPayConfiguration;

    @Before
    public void setup() throws JSONException {
        JSONObject configuration = new JSONObject(stringFromFixture("configuration_with_unionpay.json"));
        mUnionPayConfiguration = configuration.getJSONObject("unionPay");
    }

    @Test
    public void fromJson_parsesWithUnionPayConfiguration() throws JSONException {
        UnionPayConfiguration unionPayConfiguration = UnionPayConfiguration.fromJson(mUnionPayConfiguration);

        assertTrue(unionPayConfiguration.isEnabled());
    }

    @Test
    public void fromJson_parsesEmptyUnionPayConfigurationIfNotDefined() throws JSONException {
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
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_unionpay.json"));

        assertTrue(configuration.getUnionPay().isEnabled());
    }

}
