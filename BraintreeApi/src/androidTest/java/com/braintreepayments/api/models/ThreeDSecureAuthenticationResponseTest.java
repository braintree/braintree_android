package com.braintreepayments.api.models;

import android.content.Intent;
import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class ThreeDSecureAuthenticationResponseTest extends AndroidTestCase {

    public void testCanInstantiateFromJsonString() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                FixturesHelper.stringFromFixture(getContext(),
                        "three_d_secure/authentication_response.json"));

        assertEquals("11", authResponse.getCard().getLastTwo());
        assertTrue(authResponse.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(authResponse.isSuccess());
    }

    public void testCanInstantiateFromJsonErrorString() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                FixturesHelper.stringFromFixture(getContext(),
                        "three_d_secure/authentication_response_with_error.json"));

        assertNull(authResponse.getCard());
        assertFalse(authResponse.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(authResponse.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertFalse(authResponse.isSuccess());
    }

    public void testCanBeSerialized() {
        ThreeDSecureAuthenticationResponse authResponse = ThreeDSecureAuthenticationResponse.fromJson(
                FixturesHelper.stringFromFixture(getContext(),
                "three_d_secure/authentication_response.json"));

        Intent intent = new Intent().putExtra("auth-response", authResponse);
        ThreeDSecureAuthenticationResponse parsedAuthResponse = intent.getParcelableExtra("auth-response");

        assertEquals(authResponse.getCard().getLastTwo(), parsedAuthResponse.getCard().getLastTwo());
        assertEquals(authResponse.getThreeDSecureInfo().isLiabilityShifted(),
                parsedAuthResponse.getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(authResponse.getThreeDSecureInfo().isLiabilityShiftPossible(),
                parsedAuthResponse.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(authResponse.isSuccess(), parsedAuthResponse.isSuccess());
    }

}
