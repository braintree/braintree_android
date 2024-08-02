package com.braintreepayments.api.paypal;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalLineItemUnitTest {

    @Test
    public void toJson_setsKeysAndValues() throws JSONException {
        PayPalLineItem item = new PayPalLineItem(PayPalLineItemKind.DEBIT,
            "An Item",
            "1",
            "2");
        item.setDescription("A new item");
        item.setImageUrl("http://example.com/image.jpg");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
        item.setUpcType(PayPalLineItemUpcType.UPC_TYPE_2);
        item.setUpcCode("upc-code");
        item.setUrl("http://example.com");

        JSONObject json = item.toJson();

        assertEquals("debit", json.getString("kind"));
        assertEquals("An Item", json.getString("name"));
        assertEquals("1", json.getString("quantity"));
        assertEquals("2", json.getString("unit_amount"));
        assertEquals("A new item", json.getString("description"));
        assertEquals("abc-123", json.getString("product_code"));
        assertEquals("1.50", json.getString("unit_tax_amount"));
        assertEquals("http://example.com", json.getString("url"));
        assertEquals("http://example.com/image.jpg", json.getString("image_url"));
        assertEquals("UPC-2", json.getString("upc_type"));
        assertEquals("upc-code", json.getString("upc_code"));
    }
}

