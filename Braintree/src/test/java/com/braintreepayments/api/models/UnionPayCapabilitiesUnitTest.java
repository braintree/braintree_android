package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class UnionPayCapabilitiesUnitTest {

    public UnionPayCapabilities mUnionPayCapabilities;

    @Before
    public void setup() {
        mUnionPayCapabilities = new UnionPayCapabilities(true, false, true, false);
    }

    @Test
    public void constructor_creates() {
        assertTrue(mUnionPayCapabilities.isUnionPay());
        assertFalse(mUnionPayCapabilities.isDebit());
        assertTrue(mUnionPayCapabilities.supportsTwoStepAuthAndCapture());
        assertFalse(mUnionPayCapabilities.isSupported());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        Parcel parcel = Parcel.obtain();

        mUnionPayCapabilities.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        UnionPayCapabilities actual = new UnionPayCapabilities(parcel);
        assertEquals(mUnionPayCapabilities.isUnionPay(), actual.isUnionPay());
        assertEquals(mUnionPayCapabilities.isDebit(), actual.isDebit());
        assertEquals(mUnionPayCapabilities.supportsTwoStepAuthAndCapture(), actual.supportsTwoStepAuthAndCapture());
        assertEquals(mUnionPayCapabilities.isSupported(), actual.isSupported());
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
