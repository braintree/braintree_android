package com.braintreepayments.api.sepadirectdebit

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SEPADirectDebitNonceTest {

    @Test
    fun parcels_withAllFieldsPopulated() {
        val nonce = SEPADirectDebitNonce(
            string = "fake-nonce-123",
            isDefault = true,
            ibanLastFour = "1234",
            customerId = "customer-abc",
            mandateType = SEPADirectDebitMandateType.RECURRENT
        )

        val parcel = Parcel.obtain()
        nonce.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<SEPADirectDebitNonce>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals("fake-nonce-123", parceled.string)
        assertEquals(true, parceled.isDefault)
        assertEquals("1234", parceled.ibanLastFour)
        assertEquals("customer-abc", parceled.customerId)
        assertEquals(SEPADirectDebitMandateType.RECURRENT, parceled.mandateType)
    }

    @Test
    fun parcels_withNullOptionalFields() {
        val nonce = SEPADirectDebitNonce(
            string = "fake-nonce-456",
            isDefault = false,
            ibanLastFour = null,
            customerId = null,
            mandateType = null
        )

        val parcel = Parcel.obtain()
        nonce.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<SEPADirectDebitNonce>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals("fake-nonce-456", parceled.string)
        assertFalse(parceled.isDefault)
        assertNull(parceled.ibanLastFour)
        assertNull(parceled.customerId)
        assertNull(parceled.mandateType)
    }

    @Test
    fun fromJSON_parsesTokenizeResponse() {
        val json = JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
        val nonce = SEPADirectDebitNonce.fromJSON(json)

        assertEquals("1194c322-9763-08b7-4777-0b9b5e5cc3e4", nonce.string)
        assertFalse(nonce.isDefault)
        assertEquals("1234", nonce.ibanLastFour)
        assertEquals("a-customer-id", nonce.customerId)
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, nonce.mandateType)
    }

    @Test
    fun fromJSON_withMissingDetails_hasNullOptionalFields() {
        val json = JSONObject("""
            {
                "nonce": "test-nonce-789"
            }
        """)
        val nonce = SEPADirectDebitNonce.fromJSON(json)

        assertEquals("test-nonce-789", nonce.string)
        assertFalse(nonce.isDefault)
        assertNull(nonce.ibanLastFour)
        assertNull(nonce.customerId)
        assertNull(nonce.mandateType)
    }
}
