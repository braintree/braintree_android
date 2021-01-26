package com.braintreepayments.api;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class Assertions {

    public static void assertIsANonce(String maybeANonce) {
        assertNotNull("Nonce was null", maybeANonce);
        assertTrue("Does not match the expected form of a nonce. Expected \"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$\" got \"" + maybeANonce + "\"",
                maybeANonce.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }

    public static void assertBinDataEqual(BinData expected, BinData actual) {
        assertEquals(expected.getPrepaid(), actual.getPrepaid());
        assertEquals(expected.getHealthcare(), actual.getHealthcare());
        assertEquals(expected.getDebit(), actual.getDebit());
        assertEquals(expected.getDurbinRegulated(), actual.getDurbinRegulated());
        assertEquals(expected.getCommercial(), actual.getCommercial());
        assertEquals(expected.getPayroll(), actual.getPayroll());
        assertEquals(expected.getIssuingBank(), actual.getIssuingBank());
        assertEquals(expected.getCountryOfIssuance(), actual.getCountryOfIssuance());
        assertEquals(expected.getProductId(), actual.getProductId());
    }
}
