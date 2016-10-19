package com.braintreepayments.api.models;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class BraintreeRequestCodesUnitTest {

    @Test
    public void visaCheckout_expectedValue() {
        assertEquals(13592, BraintreeRequestCodes.VISA_CHECKOUT);
    }
}
