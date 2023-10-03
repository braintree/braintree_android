package com.braintreepayments.api

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BraintreeRequestCodesUnitTest {

    @Test
    fun threeDSecure() {
        assertEquals(13487, BraintreeRequestCodes.THREE_D_SECURE)
    }

    @Test
    fun venmo() {
        assertEquals(13488, BraintreeRequestCodes.VENMO)
    }

    @Test
    fun paypal() {
        assertEquals(13591, BraintreeRequestCodes.PAYPAL)
    }

    @Test
    fun visaCheckout() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT)
    }

    @Test
    fun googlePay() {
        assertEquals(13593, BraintreeRequestCodes.GOOGLE_PAY)
    }

    @Test
    fun samsungPay() {
        assertEquals(13595, BraintreeRequestCodes.SAMSUNG_PAY)
    }

    @Test
    fun localPayment() {
        assertEquals(13596, BraintreeRequestCodes.LOCAL_PAYMENT)
    }

    @Test
    fun sepDebit() {
        assertEquals(13597, BraintreeRequestCodes.SEPA_DEBIT)
    }
}
