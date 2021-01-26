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

        assertNull(request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertEquals(PayPalRequest.INTENT_AUTHORIZE, request.getIntent());
        assertNull(request.getLandingPageType());
        assertFalse(request.shouldOfferCredit());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalRequest request = new PayPalRequest()
                .amount("1.00")
                .currencyCode("USD")
                .localeCode("US")
                .billingAgreementDescription("Billing Agreement Description")
                .shippingAddressRequired(true)
                .shippingAddressOverride(postalAddress)
                .intent(PayPalRequest.INTENT_SALE)
                .userAction(PayPalRequest.USER_ACTION_COMMIT)
                .displayName("Display Name")
                .landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN)
                .offerCredit(true);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalRequest.INTENT_SALE, request.getIntent());
        assertEquals(PayPalRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.shouldOfferCredit());
    }
}
