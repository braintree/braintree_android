package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.ErrorWithResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureAuthenticationResponseUnitTest {

    @Test
    public void fromJson_parsesCorrectly() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response.json"));

        assertEquals("11", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
    }

    @Test
    public void fromException_parsesCorrectly() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse
                .fromException("Error!");

        assertFalse(authResponse.isSuccess());
        assertEquals("Error!", authResponse.getException());
    }

    @Test
    public void getErrors_returnsErrorString() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response_with_error.json"));
        ErrorWithResponse errors = new ErrorWithResponse(0, authResponse.getErrors());

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment", errors.getMessage());
    }

    @Test
    public void isParcelable() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response.json"));
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAuthenticationResponse parceled = ThreeDSecureAuthenticationResponse.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.getCardNonce().getLastTwo(), parceled.getCardNonce().getLastTwo());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(authResponse.isSuccess(), parceled.isSuccess());
        assertEquals(authResponse.getException(), parceled.getException());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(), parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
    }

    @Test
    public void exceptionsAreParcelable() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse
                .fromException("Error!");
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAuthenticationResponse parceled = ThreeDSecureAuthenticationResponse.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.isSuccess(), parceled.isSuccess());
        assertEquals(authResponse.getException(), parceled.getException());
    }
}
