package com.braintreepayments.api.test;

import com.braintreepayments.api.models.BinData;

import static org.junit.Assert.assertEquals;

public class Assertions {

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
