package com.braintreepayments.api.models;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4.class)
public class BraintreeRequestCodesUnitTest {

    @Test
    public void visaCheckout_expectedValue() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT);
    }
}
