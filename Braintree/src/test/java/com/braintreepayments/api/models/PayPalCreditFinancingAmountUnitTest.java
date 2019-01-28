package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PayPalCreditFinancingAmountUnitTest {

    @Test
    public void fromJson_returnsEmptyObjectWhenNull() {
        PayPalCreditFinancingAmount creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(null);
        assertNotNull(creditFinancingAmount);
        assertNull(creditFinancingAmount.getCurrency());
        assertNull(creditFinancingAmount.getValue());
    }

    @Test
    public void canCreateCreditFinancingAmount_fromStandardJson() throws JSONException {
        String json = "{\"currency\": \"USD\", \"value\": \"123.45\"}";
        PayPalCreditFinancingAmount creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(new JSONObject(json));

        assertEquals("USD", creditFinancingAmount.getCurrency());
        assertEquals("123.45", creditFinancingAmount.getValue());
    }

    @Test
    public void canCreateCreditFinancingAmount_fromJsonMissingCurrency() throws JSONException {
        String json = "{\"value\": \"123.45\"}";
        PayPalCreditFinancingAmount creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(new JSONObject(json));

        assertNull(creditFinancingAmount.getCurrency());
        assertEquals("123.45", creditFinancingAmount.getValue());
    }

    @Test
    public void canCreateCreditFinancingAmount_fromJsonMissingValue() throws JSONException {
        String json = "{\"currency\": \"USD\"}";
        PayPalCreditFinancingAmount creditFinancingAmount = PayPalCreditFinancingAmount.fromJson(new JSONObject(json));

        assertNull(creditFinancingAmount.getValue());
        assertEquals("USD", creditFinancingAmount.getCurrency());
    }

    @Test
    public void writeToParcel_serializesCorrectly() throws JSONException {
        String json = "{\"currency\": \"USD\", \"value\": \"123.45\"}";
        PayPalCreditFinancingAmount preSerialized = PayPalCreditFinancingAmount.fromJson(new JSONObject(json));

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalCreditFinancingAmount creditFinancingAmount = PayPalCreditFinancingAmount.CREATOR.createFromParcel(parcel);

        assertNotNull(creditFinancingAmount);
        assertEquals("USD", creditFinancingAmount.getCurrency());
        assertEquals("123.45", creditFinancingAmount.getValue());
    }
}
