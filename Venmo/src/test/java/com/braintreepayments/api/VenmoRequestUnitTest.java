package com.braintreepayments.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VenmoRequestUnitTest {

    @Test
    public void constructor_defaultsPaymentMethodUsageTypeToUnspecified() {
        VenmoRequest sut = new VenmoRequest();
        assertEquals(VenmoPaymentMethodUsage.UNSPECIFIED, sut.getPaymentMethodUsage());
    }

    @Test
    public void getPaymentMethodUsageAsString_whenUnspecified_returnsNull() {
        VenmoRequest sut = new VenmoRequest();
        sut.setPaymentMethodUsage(VenmoPaymentMethodUsage.UNSPECIFIED);
        assertNull(sut.getPaymentMethodUsageAsString());
    }

    @Test
    public void getPaymentMethodUsageAsString_whenSingleUse_returnsStringEquivalent() {
        VenmoRequest sut = new VenmoRequest();
        sut.setPaymentMethodUsage(VenmoPaymentMethodUsage.SINGLE_USE);
        assertEquals("SINGLE_USE", sut.getPaymentMethodUsageAsString());
    }

    @Test
    public void getPaymentMethodUsageAsString_whenMultiUse_returnsStringEquivalent() {
        VenmoRequest sut = new VenmoRequest();
        sut.setPaymentMethodUsage(VenmoPaymentMethodUsage.MULTI_USE);
        assertEquals("MULTI_USE", sut.getPaymentMethodUsageAsString());
    }
}