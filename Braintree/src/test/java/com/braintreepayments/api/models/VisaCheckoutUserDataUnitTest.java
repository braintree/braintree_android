package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutUserDataUnitTest {

    private VisaCheckoutUserData mVisaCheckoutUserData;

    @Before
    public void setup() throws JSONException {
       JSONObject sampleUserData = new JSONObject()
               .put("userFirstName", "userFirstName")
               .put("userLastName", "userLastName")
               .put("userFullName", "userFullName")
               .put("userName", "userName")
               .put("userEmail", "userEmail");

        mVisaCheckoutUserData = new VisaCheckoutUserData(sampleUserData);
    }

    @Test
    public void fillsOutFromJson() {
        assertEquals("userFirstName", mVisaCheckoutUserData.getUserFirstName());
        assertEquals("userLastName", mVisaCheckoutUserData.getUserLastName());
        assertEquals("userFullName", mVisaCheckoutUserData.getUserFullName());
        assertEquals("userName", mVisaCheckoutUserData.getUserName());
        assertEquals("userEmail", mVisaCheckoutUserData.getUserEmail());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        mVisaCheckoutUserData.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutUserData actual = VisaCheckoutUserData.CREATOR.createFromParcel(parcel);

        assertEquals(mVisaCheckoutUserData.getUserFirstName(), actual.getUserFirstName());
        assertEquals(mVisaCheckoutUserData.getUserLastName(), actual.getUserLastName());
        assertEquals(mVisaCheckoutUserData.getUserFullName(), actual.getUserFullName());
        assertEquals(mVisaCheckoutUserData.getUserName(), actual.getUserName());
        assertEquals(mVisaCheckoutUserData.getUserEmail(), actual.getUserEmail());
    }
}
