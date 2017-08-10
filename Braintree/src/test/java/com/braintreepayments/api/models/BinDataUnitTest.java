package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.testutils.Assertions.assertBinDataEqual;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class BinDataUnitTest {

    @Test
    public void parsesCorrectly_ifEmptyJson() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject("{}"));

        assertNotNull(binData);
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(UNKNOWN, binData.getHealthcare());
        assertEquals(UNKNOWN, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void parsesCorrectly_ifNull() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject());

        assertNotNull(binData);
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(UNKNOWN, binData.getHealthcare());
        assertEquals(UNKNOWN, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void isParcelable() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject(stringFromFixture("bin_data.json")));
        Parcel parcel = Parcel.obtain();
        binData.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        BinData parceled = BinData.CREATOR.createFromParcel(parcel);

        assertBinDataEqual(binData, parceled);
    }
}
