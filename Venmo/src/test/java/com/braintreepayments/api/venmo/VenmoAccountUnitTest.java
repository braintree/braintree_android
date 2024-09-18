package com.braintreepayments.api.venmo;

import static junit.framework.Assert.assertEquals;

import com.braintreepayments.api.core.IntegrationType;
import com.braintreepayments.api.core.PaymentMethod;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountUnitTest {

    @Test
    public void correctlyBuildsVenmoVaultRequest() throws JSONException {
        VenmoAccount sut = new VenmoAccount("some-nonce", null, PaymentMethod.DEFAULT_SOURCE, IntegrationType.CUSTOM);

        JSONObject fullJson = sut.buildJSON();
        JSONObject venmoAccountJson = fullJson.getJSONObject("venmoAccount");
        assertEquals("some-nonce", venmoAccountJson.getString("nonce"));
    }
}
