package com.braintreepayments.testutils;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class Assertions {

    public static void assertIsANonce(String maybeANonceA) {
        assertNotNull("Nonce was null", maybeANonceA);
        assertTrue("Does not match the expected form of a nonce",
                maybeANonceA.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }
}
