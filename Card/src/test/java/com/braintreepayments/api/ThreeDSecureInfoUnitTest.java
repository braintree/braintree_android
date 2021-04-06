package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ThreeDSecureInfoUnitTest {

    @Test
    public void canCreateThreeDSecureInfoFromJson() throws JSONException {
        JSONObject json = new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD);
        ThreeDSecureInfo info = ThreeDSecureInfo.fromJson(
                json.getJSONArray("creditCards")
                        .getJSONObject(0)
                        .getJSONObject("threeDSecureInfo"));

        assertEquals("fake-cavv", info.getCavv());
        assertEquals("fake-txn-id", info.getDsTransactionId());
        assertEquals("07", info.getEciFlag());
        assertEquals("Y", info.getEnrolled());
        assertTrue(info.isLiabilityShiftPossible());
        assertFalse(info.isLiabilityShifted());
        assertEquals("lookup_enrolled", info.getStatus());
        assertEquals("2.2.0", info.getThreeDSecureVersion());
        assertTrue(info.wasVerified());
        assertEquals("fake-xid", info.getXid());
        assertEquals("fake-acs-transaction-id", info.getAcsTransactionId());
        assertEquals("fake-threedsecure-authentication-id", info.getThreeDSecureAuthenticationId());
        assertEquals("fake-threedsecure-server-transaction-id", info.getThreeDSecureServerTransactionId());
        assertEquals("fake-pares-status", info.getParesStatus());
        assertEquals("Y", info.getAuthenticationTransactionStatus());
        assertEquals("01", info.getAuthenticationTransactionStatusReason());
        assertEquals("N", info.getLookupTransactionStatus());
        assertEquals("02", info.getLookupTransactionStatusReason());
    }
}
