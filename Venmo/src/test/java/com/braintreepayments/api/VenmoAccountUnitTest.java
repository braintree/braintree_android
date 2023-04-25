package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountUnitTest {

    @Test
    public void correctlyBuildsVenmoVaultRequest() throws JSONException {
        VenmoAccount sut = new VenmoAccount();
        sut.setNonce("some-nonce");

        JSONObject fullJson = sut.buildJSON();
        JSONObject venmoAccountJson = fullJson.getJSONObject("venmoAccount");
        assertEquals("some-nonce", venmoAccountJson.getString("nonce"));
    }
}
