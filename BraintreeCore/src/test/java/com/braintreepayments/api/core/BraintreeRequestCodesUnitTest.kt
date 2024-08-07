package com.braintreepayments.api.core

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BraintreeRequestCodesUnitTest {

    @Test
    fun threeDSecure() {
        assertEquals(13487, BraintreeRequestCodes.THREE_D_SECURE.code)
    }

    @Test
    fun venmo() {
        assertEquals(13488, BraintreeRequestCodes.VENMO.code)
    }

    @Test
    fun paypal() {
        assertEquals(13591, BraintreeRequestCodes.PAYPAL.code)
    }

    @Test
    fun visaCheckout() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT.code)
    }

    @Test
    fun googlePay() {
        assertEquals(13593, BraintreeRequestCodes.GOOGLE_PAY.code)
    }

    @Test
    fun localPayment() {
        assertEquals(13596, BraintreeRequestCodes.LOCAL_PAYMENT.code)
    }

    @Test
    fun sepDebit() {
        assertEquals(13597, BraintreeRequestCodes.SEPA_DEBIT.code)
    }
}
