package com.braintreepayments.api.threedsecure;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import android.os.Parcel;

import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureParamsUnitTest {

    @Test
    public void fromJson_parsesCorrectly_v1() throws JSONException {
        ThreeDSecureParams authResponse = ThreeDSecureParams.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);

        assertEquals("11", authResponse.getThreeDSecureNonce().getLastTwo());
        assertTrue(authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted());
        assertTrue(
                authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_parsesCorrectly_v2() throws JSONException {
        ThreeDSecureParams authResponse = ThreeDSecureParams.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE);

        assertEquals("91", authResponse.getThreeDSecureNonce().getLastTwo());
        assertTrue(authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted());
        assertTrue(
                authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v1() throws JSONException {
        ThreeDSecureParams authResponse = ThreeDSecureParams.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getThreeDSecureNonce());
        assertEquals("Failed to authenticate, please try a different form of payment.",
                authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v2() throws JSONException {
        ThreeDSecureParams authResponse = ThreeDSecureParams.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getThreeDSecureNonce());
        assertEquals("Failed to authenticate, please try a different form of payment.",
                authResponse.getErrorMessage());
    }

    @Test
    public void isParcelable() throws JSONException {
        ThreeDSecureParams authResponse = ThreeDSecureParams.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureParams parceled = ThreeDSecureParams.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.getThreeDSecureNonce().getLastTwo(),
                parceled.getThreeDSecureNonce().getLastTwo());
        assertEquals(authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted(),
                parceled.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted());
        assertEquals(
                authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible(),
                parceled.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible());
        assertEquals(authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted(),
                parceled.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShifted());
        assertEquals(
                authResponse.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible(),
                parceled.getThreeDSecureNonce().getThreeDSecureInfo().getLiabilityShiftPossible());
    }
}
