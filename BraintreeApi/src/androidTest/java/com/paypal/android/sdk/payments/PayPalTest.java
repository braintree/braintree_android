package com.paypal.android.sdk.payments;

import android.test.AndroidTestCase;

import com.braintreepayments.api.TestUtils;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.google.gson.Gson;

import org.json.JSONException;

public class PayPalTest extends AndroidTestCase {

    public void testAdapterProxiesAuthorizationCode() throws ConfigurationException, JSONException {
        PayPalAccountBuilder accountBuilder = TestUtils.fakePayPalAccountBuilder();
        String json = new Gson().toJson(accountBuilder.toJsonString());
        assertTrue(json.contains("fake_code"));
    }

}
