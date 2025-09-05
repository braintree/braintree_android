package com.braintreepayments.api.threedsecure

import android.os.Parcel
import com.braintreepayments.api.card.BinType
import com.braintreepayments.api.testutils.Assertions.assertBinDataEqual
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureNonceUnitTest {

    @Test
    fun `fromJSON parses ThreeDSecureNonce`() {
        val sut = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))

        assertEquals("Visa", sut.cardType)
        assertEquals("123456-12345-12345-a-adfa", sut.string)
        assertEquals("11", sut.lastTwo)
        assertEquals("1111", sut.lastFour)
        assertNotNull(sut.threeDSecureInfo)
        assertFalse { sut.threeDSecureInfo.liabilityShifted }
        assertTrue { sut.threeDSecureInfo.liabilityShiftPossible }
        assertNotNull(sut.binData)
        assertEquals(BinType.Unknown, sut.binData.prepaid)
        assertEquals(BinType.Yes, sut.binData.healthcare)
        assertEquals(BinType.No, sut.binData.debit)
        assertEquals(BinType.Unknown, sut.binData.durbinRegulated)
        assertEquals(BinType.Unknown, sut.binData.commercial)
        assertEquals(BinType.Unknown, sut.binData.payroll)
        assertEquals(BinType.Unknown.name, sut.binData.issuingBank)
        assertEquals("Something", sut.binData.countryOfIssuance)
        assertEquals("123", sut.binData.productId)
        assertEquals("unregulated", sut.authenticationInsight?.regulationEnvironment)
        assertEquals("01", sut.expirationMonth)
        assertEquals("2020", sut.expirationYear)
        assertEquals("Joe Smith", sut.cardholderName)
    }

    @Test
    fun `fromJSON with GraphQL tokenization response parses CardNonce`() {
        val sut = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))

        assertEquals("Visa", sut.cardType)
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", sut.string)
        assertEquals("11", sut.lastTwo)
        assertEquals("1111", sut.lastFour)
        assertNotNull(sut.threeDSecureInfo)
        assertFalse { sut.threeDSecureInfo.liabilityShifted }
        assertFalse { sut.threeDSecureInfo.liabilityShiftPossible }
        assertNotNull(sut.binData)
        assertEquals(BinType.Yes, sut.binData.prepaid)
        assertEquals(BinType.Yes, sut.binData.healthcare)
        assertEquals(BinType.No, sut.binData.debit)
        assertEquals(BinType.Yes, sut.binData.durbinRegulated)
        assertEquals(BinType.No, sut.binData.commercial)
        assertEquals(BinType.Yes, sut.binData.payroll)
        assertEquals("Bank of America", sut.binData.issuingBank)
        assertEquals("USA", sut.binData.countryOfIssuance)
        assertEquals("123", sut.binData.productId)
        assertEquals("unregulated", sut.authenticationInsight?.regulationEnvironment)
        assertEquals("01", sut.expirationMonth)
        assertEquals("2020", sut.expirationYear)
        assertEquals("Joe Smith", sut.cardholderName)
    }

    @Test
    fun `fromJSON with GraphQL tokenization response parses ThreeDSecure with default values`() {
        val sut = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD_MISSING_VALUES))

        assertEquals("", sut.lastFour)
        assertEquals("", sut.lastTwo)
        assertEquals("Unknown", sut.cardType)
        assertNotNull(sut.threeDSecureInfo)
        assertEquals("", sut.bin)
        assertNotNull(sut.binData)
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", sut.string)
        assertFalse(sut.isDefault)
        assertNull(sut.authenticationInsight)
        assertEquals("", sut.expirationMonth)
        assertEquals("", sut.expirationYear)
        assertEquals("", sut.cardholderName)
    }

    @Test
    fun `fromJSON with GraphQL tokenization response parses unknown card responses`() {
        val sut = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_UNKNOWN_CREDIT_CARD))

        assertEquals("Unknown", sut.cardType)
        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", sut.string)
        assertEquals("", sut.lastTwo)
        assertEquals("", sut.lastFour)
        assertEquals("", sut.expirationMonth)
        assertEquals("", sut.expirationYear)
        assertEquals("", sut.cardholderName)
        assertNotNull(sut.threeDSecureInfo)
        assertFalse(sut.threeDSecureInfo.liabilityShifted)
        assertFalse(sut.threeDSecureInfo.liabilityShiftPossible)
        assertNotNull(sut.binData)
        assertEquals(BinType.Unknown, sut.binData.prepaid)
        assertEquals(BinType.Unknown, sut.binData.healthcare)
        assertEquals(BinType.Unknown, sut.binData.debit)
        assertEquals(BinType.Unknown, sut.binData.durbinRegulated)
        assertEquals(BinType.Unknown, sut.binData.commercial)
        assertEquals(BinType.Unknown, sut.binData.payroll)
        assertEquals(BinType.Unknown.name, sut.binData.issuingBank)
        assertEquals(BinType.Unknown.name, sut.binData.countryOfIssuance)
        assertEquals(BinType.Unknown.name, sut.binData.productId)
    }

    @Test
    fun `parcels correctly`() {
        val sut = ThreeDSecureNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))

        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<ThreeDSecureNonce>().createFromParcel(parcel)

        assertEquals("Visa", parceled.cardType)
        assertEquals("123456-12345-12345-a-adfa", parceled.string)
        assertEquals("11", parceled.lastTwo)
        assertEquals("1111", parceled.lastFour)
        assertEquals("01", parceled.expirationMonth)
        assertEquals("2020", parceled.expirationYear)
        assertEquals("Joe Smith", parceled.cardholderName)
        assertFalse(parceled.isDefault)
        assertBinDataEqual(sut.binData, parceled.binData)
        assertEquals(sut.authenticationInsight?.regulationEnvironment,
            parceled.authenticationInsight?.regulationEnvironment)
        assertEquals(sut.threeDSecureInfo.liabilityShifted, parceled.threeDSecureInfo.liabilityShifted)
        assertEquals(sut.threeDSecureInfo.liabilityShiftPossible, parceled.threeDSecureInfo.liabilityShiftPossible)
    }
}
