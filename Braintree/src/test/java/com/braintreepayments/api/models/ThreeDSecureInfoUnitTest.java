package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ThreeDSecureInfoUnitTest {

    @Test
    public void canCreateThreeDSecureInfoFromJson() throws JSONException {
        JSONObject json = new JSONObject(stringFromFixture("payment_methods/visa_credit_card_response.json"));
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
        assertEquals("fake-threedsecure-server-transaction-id", info.getThreeDSecureServerTransactionId());
        assertEquals("fake-pares-status", info.getParesStatus());
    }
}
