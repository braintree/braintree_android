package com.braintreepayments.api.card

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CardNonceUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `created cardNonce from JSON with plain card nonce and parses it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHOD_CARD))

        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
        assertEquals("Visa", cardNonce.cardType)
        assertEquals("11", cardNonce.lastTwo)
        assertEquals("1111", cardNonce.lastFour)
        assertTrue(cardNonce.isDefault)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates cardNonce from RESTful tokenization response and parses it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))

        assertEquals("123456-12345-12345-a-adfa", cardNonce.string)
        assertEquals("Visa", cardNonce.cardType)
        assertEquals("11", cardNonce.lastTwo)
        assertEquals("1111", cardNonce.lastFour)
        assertNotNull(cardNonce.binData)
        assertEquals(BinType.Unknown, cardNonce.binData.prepaid)
        assertEquals(BinType.Yes, cardNonce.binData.healthcare)
        assertEquals(BinType.No, cardNonce.binData.debit)
        assertEquals(BinType.Unknown, cardNonce.binData.durbinRegulated)
        assertEquals(BinType.Unknown, cardNonce.binData.commercial)
        assertEquals(BinType.Unknown, cardNonce.binData.payroll)
        assertEquals(BinType.Unknown.name, cardNonce.binData.issuingBank)
        assertEquals("Something", cardNonce.binData.countryOfIssuance)
        assertEquals("123", cardNonce.binData.productId)
        assertEquals("unregulated", cardNonce.authenticationInsight?.regulationEnvironment)
        assertEquals("01", cardNonce.expirationMonth)
        assertEquals("2020", cardNonce.expirationYear)
        assertEquals("Joe Smith", cardNonce.cardholderName)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates cardNonce from GraphQL tokenization response and parses it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD))

        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.string)
        assertEquals("Visa", cardNonce.cardType)
        assertEquals("11", cardNonce.lastTwo)
        assertEquals("1111", cardNonce.lastFour)
        assertNotNull(cardNonce.binData)
        assertEquals(BinType.Yes, cardNonce.binData.prepaid)
        assertEquals(BinType.Yes, cardNonce.binData.healthcare)
        assertEquals(BinType.No, cardNonce.binData.debit)
        assertEquals(BinType.Yes, cardNonce.binData.durbinRegulated)
        assertEquals(BinType.No, cardNonce.binData.commercial)
        assertEquals(BinType.Yes, cardNonce.binData.payroll)
        assertEquals("Bank of America", cardNonce.binData.issuingBank)
        assertEquals("USA", cardNonce.binData.countryOfIssuance)
        assertEquals("123", cardNonce.binData.productId)
        assertEquals("unregulated", cardNonce.authenticationInsight?.regulationEnvironment)
        assertEquals("01", cardNonce.expirationMonth)
        assertEquals("2020", cardNonce.expirationYear)
        assertEquals("Joe Smith", cardNonce.cardholderName)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates cardNonce from GraphQL tokenization response with missing values and parses it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD_MISSING_VALUES))

        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.string)
        assertEquals("Unknown", cardNonce.cardType)
        assertEquals("", cardNonce.lastTwo)
        assertEquals("", cardNonce.lastFour)
        assertEquals("", cardNonce.bin)
        assertNotNull(cardNonce.binData)
        assertFalse(cardNonce.isDefault)
        assertNull(cardNonce.authenticationInsight)
        assertEquals("", cardNonce.expirationMonth)
        assertEquals("", cardNonce.expirationYear)
        assertEquals("", cardNonce.cardholderName)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates cardNonce from GraphQL tokenization response with unknown card and parses it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.GRAPHQL_RESPONSE_UNKNOWN_CREDIT_CARD))

        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", cardNonce.string)
        assertEquals("Unknown", cardNonce.cardType)
        assertEquals("", cardNonce.lastTwo)
        assertEquals("", cardNonce.lastFour)
        assertEquals("", cardNonce.expirationMonth)
        assertEquals("", cardNonce.expirationYear)
        assertEquals("", cardNonce.cardholderName)
        assertNotNull(cardNonce.binData)
        assertEquals(BinType.Unknown, cardNonce.binData.prepaid)
        assertEquals(BinType.Unknown, cardNonce.binData.healthcare)
        assertEquals(BinType.Unknown, cardNonce.binData.debit)
        assertEquals(BinType.Unknown, cardNonce.binData.durbinRegulated)
        assertEquals(BinType.Unknown, cardNonce.binData.commercial)
        assertEquals(BinType.Unknown, cardNonce.binData.payroll)
        assertEquals(BinType.Unknown.name, cardNonce.binData.issuingBank)
        assertEquals(BinType.Unknown.name, cardNonce.binData.countryOfIssuance)
        assertEquals(BinType.Unknown.name, cardNonce.binData.productId)
    }

    @Test
    @Throws(JSONException::class)
    fun `created cardNonce from JSON with Visa credit card and parcels it correctly`() {
        val cardNonce = CardNonce.fromJSON(JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD))

        val parcel = Parcel.obtain().apply {
            cardNonce.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<CardNonce>().createFromParcel(parcel)
        assertEquals("123456-12345-12345-a-adfa", parceled.string)
        assertEquals("Visa", parceled.cardType)
        assertEquals("11", parceled.lastTwo)
        assertEquals("1111", parceled.lastFour)
        assertEquals("01", parceled.expirationMonth)
        assertEquals("2020", parceled.expirationYear)
        assertEquals("Joe Smith", parceled.cardholderName)
        assertFalse(parceled.isDefault)
        assertEquals(cardNonce.binData, parceled.binData)
        assertEquals(cardNonce.authenticationInsight?.regulationEnvironment,
            parceled.authenticationInsight?.regulationEnvironment)
    }
}
