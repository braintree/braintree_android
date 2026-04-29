package com.braintreepayments.api.threedsecure

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureNonceTest {

    @Test
    fun fromJSON_parsesRESTResponse() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD)
        val nonce = ThreeDSecureNonce.fromJSON(json)

        assertEquals("Visa", nonce.cardType)
        assertEquals("11", nonce.lastTwo)
        assertEquals("1111", nonce.lastFour)
        assertEquals("123456-12345-12345-a-adfa", nonce.string)
        assertFalse(nonce.isDefault)

        val info = nonce.threeDSecureInfo
        assertEquals("fake-cavv", info.cavv)
        assertEquals("07", info.eciFlag)
        assertEquals("Y", info.enrolled)
        assertTrue(info.liabilityShiftPossible)
        assertFalse(info.liabilityShifted)
        assertTrue(info.wasVerified)
        assertEquals("Y", info.authenticationTransactionStatus)
        assertEquals("N", info.lookupTransactionStatus)
    }

    @Test
    fun fromJSON_parsesGraphQLResponse() {
        val nonce = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))

        assertEquals("Visa", nonce.cardType)
        assertEquals("1111", nonce.lastFour)
        assertNotNull(nonce.threeDSecureInfo)
    }

    @Test
    fun parcels_correctly() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD)
        val original = ThreeDSecureNonce.fromJSON(json)

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureNonce>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original.string, restored.string)
        assertEquals(original.cardType, restored.cardType)
        assertEquals(original.lastTwo, restored.lastTwo)
        assertEquals(original.lastFour, restored.lastFour)
        assertEquals(original.bin, restored.bin)
        assertEquals(original.threeDSecureInfo, restored.threeDSecureInfo)
    }
}
