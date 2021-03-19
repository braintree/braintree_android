package com.braintreepayments.api;

import android.os.Parcel;

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
    public void fromJson_parsesCorrectly_v1() {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);

        assertEquals("11", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_parsesCorrectly_v2() {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE);

        assertEquals("91", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
        assertNull(authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v1() {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrorMessage());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v2() {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR);

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrorMessage());
    }

    @Test
    public void isParcelable() {
        ThreeDSecureResult authResponse = ThreeDSecureResult.fromJson(
                Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        Parcel parcel = Parcel.obtain();
        authResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureResult parceled = ThreeDSecureResult.CREATOR.createFromParcel(parcel);

        assertEquals(authResponse.getCardNonce().getLastTwo(), parceled.getCardNonce().getLastTwo());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(authResponse.isSuccess(), parceled.isSuccess());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(), parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
    }
}
