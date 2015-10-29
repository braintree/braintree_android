package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.ErrorWithResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureAuthenticationResponseTest {

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesCorrectly() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response.json"));

        assertEquals("11", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromException_parsesCorrectly() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse
                .fromException("Error!");

        assertFalse(authResponse.isSuccess());
        assertEquals("Error!", authResponse.getException());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getErrors_returnsErrorString() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response_with_error.json"));
        ErrorWithResponse errors = new ErrorWithResponse(0, authResponse.getErrors());

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment", errors.getMessage());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void isParcelable() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response.json"));
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAuthenticationResponse parsedAuthResponse = ThreeDSecureAuthenticationResponse.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.getCardNonce().getLastTwo(), parsedAuthResponse.getCardNonce().getLastTwo());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parsedAuthResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parsedAuthResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(authResponse.isSuccess(), parsedAuthResponse.isSuccess());
        assertEquals(authResponse.getException(), parsedAuthResponse.getException());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void exceptionsAreParcelable() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse
                .fromException("Error!");
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAuthenticationResponse parsedAuthResponse = ThreeDSecureAuthenticationResponse.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.isSuccess(), parsedAuthResponse.isSuccess());
        assertEquals(authResponse.getException(), parsedAuthResponse.getException());
    }
}
