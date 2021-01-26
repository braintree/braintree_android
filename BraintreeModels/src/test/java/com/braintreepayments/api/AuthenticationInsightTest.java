package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.api.AuthenticationInsight;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class AuthenticationInsightTest {
    @Test
    public void fromJson_successfullyParsesPSDTWO() throws JSONException {
        JSONObject response = new JSONObject()
                .put("customerAuthenticationRegulationEnvironment", "psdtwo");

        AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(response);

        assertEquals("psd2", authenticationInsight.getRegulationEnvironment());
    }
    @Test
    public void fromJson_onUnknownRegulationEnvironment_returnsUnknown() throws JSONException {
        JSONObject response = new JSONObject()
                .put("customerAuthenticationRegulationEnvironment", "FaKeVaLuE");

        AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(response);

        assertEquals("fakevalue", authenticationInsight.getRegulationEnvironment());
    }

    @Test
    public void fromJson_withRegulationEnvironmentKey_returnsValue() throws JSONException {
        JSONObject response = new JSONObject()
                .put("regulationEnvironment", "UNREGULATED");

        AuthenticationInsight authenticationInsight = AuthenticationInsight.fromJson(response);

        assertEquals("unregulated", authenticationInsight.getRegulationEnvironment());
    }

    @Test
    public void fromJson_onNullJsonObject_returnsNull() {
        assertNull(AuthenticationInsight.fromJson(null));
    }

    @Test
    public void createFromParcel_withNullRegulationEnvironment_setsRegulationEnvironmentToUnknown() {
        AuthenticationInsight insight = AuthenticationInsight.CREATOR.createFromParcel(Parcel.obtain());

        assertEquals(null, insight.getRegulationEnvironment());
    }

    @Test
    public void parcelsCorrectly() throws JSONException{
        AuthenticationInsight authInsight = AuthenticationInsight.fromJson(new JSONObject()
                .put("regulationEnvironment", "psdtwo"));

        Parcel parcel = Parcel.obtain();
        authInsight.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        AuthenticationInsight parceledInsight = AuthenticationInsight.CREATOR.createFromParcel(parcel);

        assertEquals(authInsight.getRegulationEnvironment(), parceledInsight.getRegulationEnvironment());
    }
}
