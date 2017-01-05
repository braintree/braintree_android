package com.braintreepayments.api.test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class Assertions {

    public static void assertIsANonce(String maybeANonce) {
        assertNotNull("Nonce was null", maybeANonce);
        assertTrue("Does not match the expected form of a nonce. Expected \"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$\" got \"" + maybeANonce + "\"",
                maybeANonce.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }
}
