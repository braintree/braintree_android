package com.braintreepayments.api;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * Created by pair on 9/14/16.
 */
public class VisaCheckoutUnitTest {

    @Test
    public void isEnabled_returnsTrueWhenDependencyIsAdded() {
        assertTrue(VisaCheckout.isEnabled());
    }
}
