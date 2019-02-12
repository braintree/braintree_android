package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalLineItemUnitTest {

    @Test
    public void parcelsCorrectly() {
        PayPalLineItem item = new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "2");
        item.setDescription("A new item");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
        item.setUrl("http://example.com");

        Parcel parcel = Parcel.obtain();
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalLineItem result = PayPalLineItem.CREATOR.createFromParcel(parcel);

        assertEquals("debit", result.getKind());
        assertEquals("An Item", result.getName());
        assertEquals("1", result.getQuantity());
        assertEquals("2", result.getUnitAmount());
        assertEquals("A new item", result.getDescription());
        assertEquals("abc-123", result.getProductCode());
        assertEquals("1.50", result.getUnitTaxAmount());
        assertEquals("http://example.com", result.getUrl());
    }

    @Test
    public void toJson_setsKeysAndValues() throws JSONException {
        PayPalLineItem item = new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "2");
        item.setDescription("A new item");
        item.setProductCode("abc-123");
        item.setUnitTaxAmount("1.50");
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
    }
}

