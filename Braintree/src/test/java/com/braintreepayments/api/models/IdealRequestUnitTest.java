package com.braintreepayments.api.models;

import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class IdealRequestUnitTest {

    @Test
    public void build_setsAllParams() throws NoSuchFieldException, IllegalAccessException, JSONException {
        Configuration configuration = new TestConfigurationBuilder()
                .assetsUrl("http://assets.example.com")
                .buildConfiguration();

        List<IdealBank> banks = IdealBank.fromJson(configuration, stringFromFixture("payment_methods/ideal_issuing_banks.json"));

        IdealRequest builder = new IdealRequest()
                .issuerId(banks.get(0).getId())
                .orderId("some-order-id")
                .amount("1.00")
                .currency("USD");

        JSONObject json = new JSONObject(builder.build("http://example.com", "some-route-id"));

        assertEquals("some-route-id", json.getString("route_id"));
        assertEquals("some-order-id", json.getString("order_id"));
        assertEquals("ABNANL2A", json.getString("issuer"));
        assertEquals("1.00", json.getString("amount"));
        assertEquals("USD", json.getString("currency"));
        assertEquals("http://example.com", json.getString("redirect_url"));
    }
}
