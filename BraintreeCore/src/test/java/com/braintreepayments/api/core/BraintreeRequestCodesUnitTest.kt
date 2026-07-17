package com.braintreepayments.api.core

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BraintreeRequestCodesUnitTest {

    @Test
    fun `THREE_D_SECURE request code is 13487`() {
        assertEquals(13487, BraintreeRequestCodes.THREE_D_SECURE.code)
    }

    @Test
    fun `VENMO request code is 13488`() {
        assertEquals(13488, BraintreeRequestCodes.VENMO.code)
    }

    @Test
    fun `PAYPAL request code is 13591`() {
        assertEquals(13591, BraintreeRequestCodes.PAYPAL.code)
    }

    @Test
    fun `VISA_CHECKOUT request code is 13592`() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT.code)
    }

    @Test
    fun `GOOGLE_PAY request code is 13593`() {
        assertEquals(13593, BraintreeRequestCodes.GOOGLE_PAY.code)
    }

    @Test
    fun `LOCAL_PAYMENT request code is 13596`() {
        assertEquals(13596, BraintreeRequestCodes.LOCAL_PAYMENT.code)
    }

    @Test
    fun `SEPA_DEBIT request code is 13597`() {
        assertEquals(13597, BraintreeRequestCodes.SEPA_DEBIT.code)
    }
}
