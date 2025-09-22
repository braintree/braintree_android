package com.braintreepayments.api.threedsecure

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.parcelize.parcelableCreator

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureParamsUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecure_V1 from JSON and parses it correctly`() {
        val authResponse = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)

        assertEquals("11", authResponse.threeDSecureNonce?.lastTwo)
        assertTrue(authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShifted == true)
        assertTrue(authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShiftPossible == true)
        assertNull(authResponse.errorMessage)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecure_V2 from JSON and parses it correctly`() {
        val authResponse = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE)

        assertEquals("91", authResponse.threeDSecureNonce?.lastTwo)
        assertTrue(authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShifted == true)
        assertTrue(authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShiftPossible == true)
        assertNull(authResponse.errorMessage)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecure_V1 with AuthenticationError and parses it correctly`() {
        val authResponse = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR)

        assertNull(authResponse.threeDSecureNonce)
        assertEquals(
            "Failed to authenticate, please try a different form of payment.",
            authResponse.errorMessage
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecure_V2 with AuthenticationError and parses it correctly`() {
        val authResponse = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR)

        assertNull(authResponse.threeDSecureNonce)
        assertEquals(
            "Failed to authenticate, please try a different form of payment.",
            authResponse.errorMessage
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecure_V1 from JSON and parcels it correctly`() {
        val authResponse =
            ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)

        val parcel = Parcel.obtain().apply {
            authResponse.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<ThreeDSecureParams>().createFromParcel(parcel)

        assertEquals(authResponse.threeDSecureNonce?.lastTwo, parceled.threeDSecureNonce?.lastTwo)
        assertEquals(
            authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShifted,
            parceled.threeDSecureNonce?.threeDSecureInfo?.liabilityShifted
        )
        assertEquals(
            authResponse.threeDSecureNonce?.threeDSecureInfo?.liabilityShiftPossible,
            parceled.threeDSecureNonce?.threeDSecureInfo?.liabilityShiftPossible
        )
    }
}
