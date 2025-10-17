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
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class BinDataUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `parses correctly with an empty JSON`() {
        val binData = BinData.fromJson(JSONObject("{}"))

        assertNotNull(binData)
        assertEquals(BinType.Unknown, binData.prepaid)
        assertEquals(BinType.Unknown, binData.healthcare)
        assertEquals(BinType.Unknown, binData.debit)
        assertEquals(BinType.Unknown, binData.durbinRegulated)
        assertEquals(BinType.Unknown, binData.commercial)
        assertEquals(BinType.Unknown, binData.payroll)
        assertEquals("", binData.issuingBank)
        assertEquals("", binData.countryOfIssuance)
        assertEquals("", binData.productId)
    }

    @Test
    fun `parses correctly with a Null JSON object`() {
        val binData = BinData.fromJson(JSONObject())

        assertNotNull(binData)
        assertEquals(BinType.Unknown, binData.prepaid)
        assertEquals(BinType.Unknown, binData.healthcare)
        assertEquals(BinType.Unknown, binData.debit)
        assertEquals(BinType.Unknown, binData.durbinRegulated)
        assertEquals(BinType.Unknown, binData.commercial)
        assertEquals(BinType.Unknown, binData.payroll)
        assertEquals("", binData.issuingBank)
        assertEquals("", binData.countryOfIssuance)
        assertEquals("", binData.productId)
    }

    @Test
    @Throws(JSONException::class)
    fun `parses correctly with some Null values`() {
        val jsonObject = JSONObject(Fixtures.BIN_DATA).apply {
            put("issuingBank", JSONObject.NULL)
            put("countryOfIssuance", JSONObject.NULL)
            put("productId", JSONObject.NULL)
        }
        val binData = BinData.fromJson(jsonObject)

        assertEquals(BinType.Unknown.name, binData.issuingBank)
        assertEquals(BinType.Unknown.name, binData.countryOfIssuance)
        assertEquals(BinType.Unknown.name, binData.productId)
    }

    @Test
    @Throws(JSONException::class)
    fun `parcels and returns all properties correctly`() {
        val binData = BinData.fromJson(JSONObject(Fixtures.BIN_DATA))
        val parcel = Parcel.obtain().apply {
            binData.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<BinData>().createFromParcel(parcel)

        assertEquals(binData, parceled)
    }
}
