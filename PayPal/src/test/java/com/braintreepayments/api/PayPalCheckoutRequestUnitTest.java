package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PayPalCheckoutRequestUnitTest {

    @Test
    public void newPayPalCheckoutRequest_setsDefaultValues() {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");

        assertNotNull(request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertEquals(PayPalCheckoutRequest.INTENT_AUTHORIZE, request.getIntent());
        assertNull(request.getLandingPageType());
        assertNull(request.getBillingAgreementDescription());
        assertFalse(request.shouldOfferPayLater());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
        request.setCurrencyCode("USD");
        request.setOfferPayLater(true);
        request.setIntent(PayPalCheckoutRequest.INTENT_SALE);

        request.setLocaleCode("US");
        request.setRequestBillingAgreement(true);
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setUserAction(PayPalCheckoutRequest.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertTrue(request.shouldRequestBillingAgreement());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalCheckoutRequest.INTENT_SALE, request.getIntent());
        assertEquals(PayPalCheckoutRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.shouldOfferPayLater());
    }
}