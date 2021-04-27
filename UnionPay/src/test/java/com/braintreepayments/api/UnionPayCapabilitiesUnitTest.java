package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UnionPayCapabilitiesUnitTest {

    public UnionPayCapabilities unionPayCapabilities;

    @Before
    public void setup() {
        JSONObject unionPayCapabilitiesJson = new JSONObject();
        JSONObject unionPay = new JSONObject();
        try {
            unionPayCapabilitiesJson.put("isUnionPay", true);
            unionPayCapabilitiesJson.put("isDebit", false);
            unionPay.put("supportsTwoStepAuthAndCapture", true);
            unionPay.put("isSupported", false);
            unionPayCapabilitiesJson.put("unionPay", unionPay);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        unionPayCapabilities = UnionPayCapabilities.fromJson(unionPayCapabilitiesJson.toString());
    }

    @Test
    public void constructor_creates() {
        assertTrue(unionPayCapabilities.isUnionPay());
        assertFalse(unionPayCapabilities.isDebit());
        assertTrue(unionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(unionPayCapabilities.isSupported());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();

        unionPayCapabilities.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        UnionPayCapabilities actual = new UnionPayCapabilities(parcel);
        assertEquals(unionPayCapabilities.isUnionPay(), actual.isUnionPay());
        assertEquals(unionPayCapabilities.isDebit(), actual.isDebit());
        assertEquals(unionPayCapabilities.supportsTwoStepAuthAndCapture(), actual.supportsTwoStepAuthAndCapture());
        assertEquals(unionPayCapabilities.isSupported(), actual.isSupported());
    }

    @Test
    public void fromJson_creates() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("isUnionPay", true);
        jsonObject.put("isDebit", false);

        JSONObject unionPayObject = new JSONObject();
        unionPayObject.put("supportsTwoStepAuthAndCapture", true);
        unionPayObject.put("isSupported", false);

        jsonObject.put("unionPay", unionPayObject);

        UnionPayCapabilities unionPayCapabilities = UnionPayCapabilities.fromJson(jsonObject.toString());
        assertTrue(unionPayCapabilities.isUnionPay());
        assertFalse(unionPayCapabilities.isDebit());
        assertTrue(unionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(unionPayCapabilities.isSupported());
    }

    @Test
    public void fromJson_emptyJsonObject_expectsAllFalse() {
        UnionPayCapabilities unionPayCapabilities = UnionPayCapabilities.fromJson("{}");
        assertFalse(unionPayCapabilities.isUnionPay());
        assertFalse(unionPayCapabilities.isDebit());
        assertFalse(unionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(unionPayCapabilities.isSupported());
    }

    @Test
    public void fromJson_emptyUnionPay_expectMissingToBeFalse() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("isUnionPay", true);
        jsonObject.put("isDebit", true);

        UnionPayCapabilities unionPayCapabilities = UnionPayCapabilities.fromJson(jsonObject.toString());
        assertTrue(unionPayCapabilities.isUnionPay());
        assertTrue(unionPayCapabilities.isDebit());
        assertFalse(unionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(unionPayCapabilities.isSupported());
    }

    @Test
    public void fromJson_notUnionPay_expectAllFalse() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("isUnionPay", false);

        UnionPayCapabilities unionPayCapabilities = UnionPayCapabilities.fromJson(jsonObject.toString());
        assertFalse(unionPayCapabilities.isUnionPay());
        assertFalse(unionPayCapabilities.isDebit());
        assertFalse(unionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(unionPayCapabilities.isSupported());
    }
}
