package com.braintreepayments.api.card

import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.threedsecure.ThreeDSecureInfo
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThreeDSecureInfoUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `successfully creates ThreeDSecure object from JSON`() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD)
        val info = ThreeDSecureInfo.fromJson(
            json.getJSONArray("creditCards")
                .getJSONObject(0)
                .getJSONObject("threeDSecureInfo")
        )
        assertEquals("fake-cavv", info.cavv)
        assertEquals("fake-txn-id", info.dsTransactionId)
        assertEquals("07", info.eciFlag)
        assertEquals("Y", info.enrolled)
        assertTrue(info.liabilityShiftPossible)
        assertFalse(info.liabilityShifted)
        assertEquals("lookup_enrolled", info.status)
        assertEquals("2.2.0", info.threeDSecureVersion)
        assertTrue(info.wasVerified)
        assertEquals("fake-xid", info.xid)
        assertEquals("fake-acs-transaction-id", info.acsTransactionId)
        assertEquals("fake-threedsecure-authentication-id", info.threeDSecureAuthenticationId)
        assertEquals("fake-threedsecure-server-transaction-id", info.threeDSecureServerTransactionId)
        assertEquals("fake-pares-status", info.paresStatus)
        assertEquals("Y", info.authenticationTransactionStatus)
        assertEquals("01", info.authenticationTransactionStatusReason)
        assertEquals("N", info.lookupTransactionStatus)
        assertEquals("02", info.lookupTransactionStatusReason)
    }
}
