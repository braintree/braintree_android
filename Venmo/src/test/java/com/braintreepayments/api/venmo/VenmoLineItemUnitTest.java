package com.braintreepayments.api.venmo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VenmoLineItemUnitTest {

    @Test
    public void toJson_setsKeysAndValues() throws JSONException {
        VenmoLineItem item = new VenmoLineItem(VenmoLineItemKind.DEBIT, "An Item", 1, "2");
        item.setDescription("A new item");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
        item.setUrl("http://example.com");

        JSONObject json = item.toJson();

        assertEquals("DEBIT", json.getString("type"));
        assertEquals("An Item", json.getString("name"));
        assertEquals("1", json.getString("quantity"));
        assertEquals("2", json.getString("unitAmount"));
        assertEquals("A new item", json.getString("description"));
        assertEquals("abc-123", json.getString("productCode"));
        assertEquals("1.50", json.getString("unitTaxAmount"));
        assertEquals("http://example.com", json.getString("url"));
    }
}

