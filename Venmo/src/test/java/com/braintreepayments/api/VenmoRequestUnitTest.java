package com.braintreepayments.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class VenmoRequestUnitTest {

    @Test
    public void constructor_defaultsPaymentMethodUsageTypeToUNSPECIFIED() {
        VenmoRequest sut = new VenmoRequest();
        assertEquals(VenmoPaymentMethodUsage.UNSPECIFIED, sut.getPaymentMethodUsage());
    }
}