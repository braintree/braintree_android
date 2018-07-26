package com.braintreepayments.api.models;

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
    public void androidPay() {
        assertEquals(13489, BraintreeRequestCodes.ANDROID_PAY);
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
    public void googlePayment() {
        assertEquals(13593, BraintreeRequestCodes.GOOGLE_PAYMENT);
    }

    @Test
    public void ideal() {
        assertEquals(13594, BraintreeRequestCodes.IDEAL);
    }

    @Test
    public void samsungPay() {
        assertEquals(13595, BraintreeRequestCodes.SAMSUNG_PAY);
    }
}
