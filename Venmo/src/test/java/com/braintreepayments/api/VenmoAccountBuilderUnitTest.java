package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountBuilderUnitTest {

    @Test
    public void correctlyBuildsVenmoVaultRequest() throws JSONException {
        VenmoAccountBuilder builder = new VenmoAccountBuilder();
        builder.setNonce("some-nonce");
        builder.setValidate(true);

        JSONObject fullJson = new JSONObject(builder.buildJSON());
        JSONObject venmoAccountJson = fullJson.getJSONObject("venmoAccount");
        assertEquals("some-nonce", venmoAccountJson.getString("nonce"));

        JSONObject optionsJson = venmoAccountJson.getJSONObject("options");
        assertTrue(optionsJson.getBoolean("validate"));
    }
}
