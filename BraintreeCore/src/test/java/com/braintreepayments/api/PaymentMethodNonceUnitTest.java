package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodNonceUnitTest {

    @Test
    public void constructor() {
        PaymentMethodNonce nonce = new PaymentMethodNonce("fake-nonce", true);
        assertEquals("fake-nonce", nonce.getString());
        assertTrue(nonce.isDefault());
    }
}