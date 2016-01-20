package com.braintreepayments.api.models;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class PayPalRequestTest {

    @Test
    public void newPayPalRequest_setsAmountAndDefaultValues() {
        PayPalRequest request = new PayPalRequest("1.00");

        assertEquals("1.00", request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalRequest request = new PayPalRequest("1.00")
                .currencyCode("USD")
                .localeCode("US")
                .shippingAddressRequired(true)
                .shippingAddressOverride(postalAddress);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
    }
}
