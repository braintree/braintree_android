package com.braintreepayments.api.sepadirectdebit

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitNonceUnitTest {

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses response correctly`() {
        val sut = SEPADirectDebitNonce.fromJSON(JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE))

        assertEquals("1194c322-9763-08b7-4777-0b9b5e5cc3e4", sut.string)
        assertEquals("1234", sut.ibanLastFour)
        assertEquals("a-customer-id", sut.customerId)
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, sut.mandateType)
    }

    @Test
    @Throws(JSONException::class)
    fun `from JSON parses response and parcels it correctly`() {
        val sut = SEPADirectDebitNonce.fromJSON(JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE))
        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<SEPADirectDebitNonce>().createFromParcel(parcel)

        assertEquals("1194c322-9763-08b7-4777-0b9b5e5cc3e4", parceled.string)
        assertEquals("1234", parceled.ibanLastFour)
        assertEquals("a-customer-id", parceled.customerId)
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, parceled.mandateType)
    }
}
