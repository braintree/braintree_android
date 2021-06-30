package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoConfigurationUnitTest {

    @Test
    public void fromJson_parsesFullInput() throws JSONException {
        JSONObject input = new JSONObject()
                .put("accessToken", "sample-access-token")
                .put("environment", "sample-environment")
                .put("merchantId", "sample-merchant-id");

        VenmoConfiguration sut = VenmoConfiguration.fromJson(input);
        assertEquals("sample-access-token", sut.getAccessToken());
        assertEquals("sample-environment", sut.getEnvironment());
        assertEquals("sample-merchant-id", sut.getMerchantId());
        assertTrue(sut.isAccessTokenValid());
    }

    @Test
    public void fromJson_whenInputNull_returnsConfigWithDefaultValues() {
        VenmoConfiguration sut = VenmoConfiguration.fromJson(null);
        assertEquals("", sut.getAccessToken());
        assertEquals("", sut.getEnvironment());
        assertEquals("", sut.getMerchantId());
        assertFalse(sut.isAccessTokenValid());
    }

    @Test
    public void fromJson_whenInputEmpty_returnsConfigWithDefaultValues() {
        VenmoConfiguration sut = VenmoConfiguration.fromJson(new JSONObject());
        assertEquals("", sut.getAccessToken());
        assertEquals("", sut.getEnvironment());
        assertEquals("", sut.getMerchantId());
        assertFalse(sut.isAccessTokenValid());
    }
}
