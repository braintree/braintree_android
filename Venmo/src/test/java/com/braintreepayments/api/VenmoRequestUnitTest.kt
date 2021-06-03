package com.braintreepayments.api

import org.junit.Assert
import org.junit.Test

class VenmoRequestUnitTest {

    @Test
    fun getPaymentMethodUsageAsString_whenSingleUse_returnsStringEquivalent() {
        val sut = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE)
        Assert.assertEquals("SINGLE_USE", sut.paymentMethodUsageAsString)
    }

    @Test
    fun getPaymentMethodUsageAsString_whenMultiUse_returnsStringEquivalent() {
        val sut = VenmoRequest(VenmoPaymentMethodUsage.MULTI_USE)
        Assert.assertEquals("MULTI_USE", sut.paymentMethodUsageAsString)
    }
}