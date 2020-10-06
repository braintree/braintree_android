package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureAuthenticationResponseUnitTest {

    @Test
    public void fromJson_parsesCorrectly_v1() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response.json"));

        assertEquals("11", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
        assertNull(authResponse.getErrors());
        assertNull(authResponse.getException());
    }

    @Test
    public void fromJson_parsesCorrectly_v2() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/2.0/authentication_response.json"));

        assertEquals("91", authResponse.getCardNonce().getLastTwo());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
        assertNull(authResponse.getErrors());
        assertNull(authResponse.getException());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v1() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/authentication_response_with_error.json"));

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrors());
        assertNull(authResponse.getException());
    }

    @Test
    public void fromJson_whenAuthenticationErrorOccurs_parsesCorrectly_v2() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                stringFromFixture("three_d_secure/2.0/authentication_response_with_error.json"));

        assertNull(authResponse.getCardNonce());
        assertFalse(authResponse.isSuccess());
        assertEquals("Failed to authenticate, please try a different form of payment.", authResponse.getErrors());
        assertNull(authResponse.getException());
    }

    @Test
    public void getNonceWithAuthenticationDetails_returnsNewNonce_whenAuthenticationSuccessful() throws JSONException {
        CardNonce lookupNonce = CardNonce.fromJson(
                stringFromFixture("payment_methods/visa_credit_card_response.json"));
        CardNonce authenticationNonce = ThreeDSecureAuthenticationResponse.getNonceWithAuthenticationDetails(
                stringFromFixture("three_d_secure/authentication_response.json"), lookupNonce);

        assertFalse(lookupNonce.getNonce().equalsIgnoreCase(authenticationNonce.getNonce()));
        assertTrue(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().isSuccess());
        assertNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getErrors());
        assertNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getException());
    }

    //TODO: Possibly refactor after test case 13 card is usable in sand, so we can double check structure.
    @Test
    public void getNonceWithAuthenticationDetails_returnsLookupNonce_whenAuthenticationUnsuccessful_WithError() throws JSONException {
        CardNonce lookupNonce = CardNonce.fromJson(
                stringFromFixture("payment_methods/visa_credit_card_response.json"));
        CardNonce authenticationNonce = ThreeDSecureAuthenticationResponse.getNonceWithAuthenticationDetails(
                stringFromFixture("three_d_secure/authentication_response_with_error.json"), lookupNonce);

        assertTrue(lookupNonce.getNonce().equalsIgnoreCase(authenticationNonce.getNonce()));
        assertFalse(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().isSuccess());
        assertNotNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getErrors());
        assertNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getException());
    }

    @Test
    public void getNonceWithAuthenticationDetails_returnsLookupNonce_whenAuthenticationUnsuccessful_WithException() throws JSONException {
        CardNonce lookupNonce = CardNonce.fromJson(
                stringFromFixture("payment_methods/visa_credit_card_response.json"));
        CardNonce authenticationNonce = ThreeDSecureAuthenticationResponse.getNonceWithAuthenticationDetails(
                "{'bad'}", lookupNonce);

        assertTrue(lookupNonce.getNonce().equalsIgnoreCase(authenticationNonce.getNonce()));
        assertFalse(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().isSuccess());
        assertNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getErrors());
        assertNotNull(authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getException());
        assertEquals("Expected ':' after bad at character 7 of {'bad'}",
                authenticationNonce.getThreeDSecureInfo().getThreeDSecureAuthenticationResponse().getException());
    }

    @Test
    public void fromException_parsesCorrectly() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse
                .fromException("Error!");

        assertFalse(authResponse.isSuccess());
        assertEquals("Error!", authResponse.getException());
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
