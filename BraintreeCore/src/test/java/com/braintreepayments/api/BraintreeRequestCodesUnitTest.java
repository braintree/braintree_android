package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4.class)
public class BraintreeRequestCodesUnitTest {

    @Test
    public void expectedNumberOfRequestCodes() {
        assertEquals(8, BraintreeRequestCodes.class.getDeclaredFields().length);
    }

    @Test
    public void threeDSecure() {
        assertEquals(13487, BraintreeRequestCodes.THREE_D_SECURE);
    }

    @Test
    public void venmo() {
        assertEquals(13488, BraintreeRequestCodes.VENMO);
    }

    @Test
    public void paypal() {
        assertEquals(13591, BraintreeRequestCodes.PAYPAL);
    }

    @Test
    public void visaCheckout() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT);
    }

    @Test
    public void googlePay() {
        assertEquals(13593, BraintreeRequestCodes.GOOGLE_PAY);
    }

    @Test
    public void samsungPay() {
        assertEquals(13595, BraintreeRequestCodes.SAMSUNG_PAY);
    }

    @Test
    public void localPayment() {
        assertEquals(13596, BraintreeRequestCodes.LOCAL_PAYMENT);
    }

    @Test
    public void sepDebit() {
        assertEquals(13597, BraintreeRequestCodes.SEPA_DEBIT);
    }
}
