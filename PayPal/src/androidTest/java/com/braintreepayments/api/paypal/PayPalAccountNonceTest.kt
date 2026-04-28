package com.braintreepayments.api.paypal

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class PayPalAccountNonceTest {

    @Test
    fun fromJSON_withFullResponse_parsesAllFields() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)

        val nonce = PayPalAccountNonce.fromJSON(json)

        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.string)
        assertFalse(nonce.isDefault)
        assertEquals("paypalaccount@example.com", nonce.email)
        assertEquals("fake-authenticate-url", nonce.authenticateUrl)
        assertEquals("fake-order-id", nonce.paymentId)

        assertNotNull(nonce.creditFinancing)
        assertEquals(18, nonce.creditFinancing?.term)
        assertFalse(nonce.creditFinancing?.isCardAmountImmutable ?: true)
        assertTrue(nonce.creditFinancing?.hasPayerAcceptance ?: false)

        assertNotNull(nonce.billingAddress)
        assertEquals("123 Fake St.", nonce.billingAddress.streetAddress)
        assertEquals("Apt. 3", nonce.billingAddress.extendedAddress)
        assertEquals("Oakland", nonce.billingAddress.locality)
        assertEquals("CA", nonce.billingAddress.region)
        assertEquals("94602", nonce.billingAddress.postalCode)
    }

    @Test
    fun fromJSON_withoutAddresses_returnsEmptyAddresses() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_ADDRESSES)

        val nonce = PayPalAccountNonce.fromJSON(json)

        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.string)
        assertEquals("paypalaccount@example.com", nonce.email)
        assertNotNull(nonce.billingAddress)
        assertNotNull(nonce.shippingAddress)
    }

    @Test
    fun fromJSON_withCreditFinancingWithoutFullData_parsesTerm() {
        val json = JSONObject(
            Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_CREDIT_FINANCING_DATA
        )

        val nonce = PayPalAccountNonce.fromJSON(json)

        assertEquals("8d4e91d2-4627-10f4-6336-113e331ff9ce", nonce.string)
        assertEquals("paypal@vividseats.com", nonce.email)
        assertNotNull(nonce.creditFinancing)
        assertEquals(18, nonce.creditFinancing?.term)
        assertNotNull(nonce.creditFinancing?.monthlyPayment)
        assertNull(nonce.creditFinancing?.monthlyPayment?.currency)
        assertNull(nonce.creditFinancing?.monthlyPayment?.value)

        assertEquals("Vivid", nonce.firstName)
        assertEquals("Tester", nonce.lastName)
        assertEquals("408-242-6641", nonce.phone)
        assertEquals("NSXGUJGV6WMGW", nonce.payerId)

        assertNotNull(nonce.billingAddress)
        assertEquals("1 Main St", nonce.billingAddress.streetAddress)
        assertEquals("San Jose", nonce.billingAddress.locality)
    }

    @Test
    fun parcels_correctly() {
        val json = JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)
        val original = PayPalAccountNonce.fromJSON(json)

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<PayPalAccountNonce>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original.string, restored.string)
        assertEquals(original.isDefault, restored.isDefault)
        assertEquals(original.email, restored.email)
        assertEquals(original.authenticateUrl, restored.authenticateUrl)
        assertEquals(original.paymentId, restored.paymentId)
        assertEquals(original.billingAddress.streetAddress, restored.billingAddress.streetAddress)
        assertEquals(original.billingAddress.locality, restored.billingAddress.locality)
        assertEquals(original.creditFinancing?.term, restored.creditFinancing?.term)
        assertEquals(
            original.creditFinancing?.isCardAmountImmutable,
            restored.creditFinancing?.isCardAmountImmutable
        )
    }
}
