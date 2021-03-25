package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureResultUnitTest {

    @Test
    public void fromJson_parsesCorrectly_v1() throws JSONException {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);

        assertEquals("11", authResponse.getTokenizedCard().getLastTwo());
        assertTrue(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_parsesCorrectly_v2() throws JSONException {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE);

        assertEquals("91", authResponse.getTokenizedCard().getLastTwo());
        assertTrue(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v1() throws JSONException {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getTokenizedCard());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v2() throws JSONException {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getTokenizedCard());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrorMessage());
    }

    @Test
    public void isParcelable() throws JSONException {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureResult parceled = ThreeDSecureResult.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.getTokenizedCard().getLastTwo(), parceled.getTokenizedCard().getLastTwo());
        assertEquals(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted(),
                parceled.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted(), parceled.getTokenizedCard().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getTokenizedCard().getThreeDSecureInfo().isLiabilityShiftPossible());
    }
}
