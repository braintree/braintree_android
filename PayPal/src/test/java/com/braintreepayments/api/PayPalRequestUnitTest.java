package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalRequestUnitTest {

    @Test
    public void newPayPalRequest_setsDefaultValues() {
        PayPalRequest request = new PayPalRequest();

        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertNull(request.getLandingPageType());
        assertFalse(request.shouldOfferCredit());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalRequest request = new PayPalRequest();
        request.setLocaleCode("US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setUserAction(PayPalRequest.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        request.setOfferCredit(true);

        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.shouldOfferCredit());
    }
}
