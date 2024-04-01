package com.braintreepayments.api.visacheckout;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

import com.braintreepayments.api.visacheckout.VisaCheckoutUserData;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutUserDataUnitTest {

    private JSONObject sampleUserData;

    @Before
    public void setup() throws JSONException {
       sampleUserData = new JSONObject()
               .put("userFirstName", "userFirstName")
               .put("userLastName", "userLastName")
               .put("userFullName", "userFullName")
               .put("userName", "userName")
               .put("userEmail", "userEmail");
    }

    @Test
    public void fromJson_whenValid_returnsPopulatedObject() {
        VisaCheckoutUserData visaCheckoutUserData = VisaCheckoutUserData.fromJson(sampleUserData);

        assertEquals("userFirstName", visaCheckoutUserData.getUserFirstName());
        assertEquals("userLastName", visaCheckoutUserData.getUserLastName());
        assertEquals("userFullName", visaCheckoutUserData.getUserFullName());
        assertEquals("userName", visaCheckoutUserData.getUsername());
        assertEquals("userEmail", visaCheckoutUserData.getUserEmail());
    }

    @Test
    public void fromJson_whenNull_returnsEmptyObject() {
        VisaCheckoutUserData visaCheckoutUserData = VisaCheckoutUserData.fromJson(null);

        assertEquals("", visaCheckoutUserData.getUserFirstName());
        assertEquals("", visaCheckoutUserData.getUserLastName());
        assertEquals("", visaCheckoutUserData.getUserFullName());
        assertEquals("", visaCheckoutUserData.getUsername());
        assertEquals("", visaCheckoutUserData.getUserEmail());
    }

    @Test
    public void parcelsCorrectly() {
        VisaCheckoutUserData visaCheckoutUserData = VisaCheckoutUserData.fromJson(sampleUserData);

        Parcel parcel = Parcel.obtain();
        visaCheckoutUserData.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutUserData actual = VisaCheckoutUserData.CREATOR.createFromParcel(parcel);

        assertEquals(visaCheckoutUserData.getUserFirstName(), actual.getUserFirstName());
        assertEquals(visaCheckoutUserData.getUserLastName(), actual.getUserLastName());
        assertEquals(visaCheckoutUserData.getUserFullName(), actual.getUserFullName());
        assertEquals(visaCheckoutUserData.getUsername(), actual.getUsername());
        assertEquals(visaCheckoutUserData.getUserEmail(), actual.getUserEmail());
    }
}
