package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalCheckoutRequestUnitTest {

    @Test
    public void newPayPalRequest_setsDefaultValues() {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");

        assertNull(request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertEquals(PayPalCheckoutRequest.INTENT_AUTHORIZE, request.getIntent());
        assertNull(request.getLandingPageType());
        assertFalse(request.shouldOfferCredit());
        assertFalse(request.shouldOfferPayLater());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
        request.setCurrencyCode("USD");
        request.setOfferPayLater(true);
        request.setIntent(PayPalCheckoutRequest.INTENT_SALE);

        request.localeCode("US");
        request.billingAgreementDescription("Billing Agreement Description");
        request.shippingAddressRequired(true);
        request.shippingAddressOverride(postalAddress);
        request.userAction(PayPalRequest.USER_ACTION_COMMIT);
        request.displayName("Display Name");
        request.landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        request.offerCredit(true);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalCheckoutRequest.INTENT_SALE, request.getIntent());
        assertEquals(PayPalRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.shouldOfferCredit());
        assertTrue(request.shouldOfferPayLater());
    }
}