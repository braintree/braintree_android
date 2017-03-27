package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class PayPalRequestUnitTest {

    @Test
    public void newPayPalRequest_setsAmountAndDefaultValues() {
        PayPalRequest request = new PayPalRequest("1.00");

        assertEquals("1.00", request.getAmount());
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
        PayPalRequest request = new PayPalRequest("1.00")
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

    @Test
    public void testWriteToParcel_serializesCorrectly() throws JSONException {
        PostalAddress postalAddress = new PostalAddress();
        PayPalRequest expected = new PayPalRequest("1.00")
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

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalRequest actual = new PayPalRequest(parcel);

        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getBillingAgreementDescription(), actual.getBillingAgreementDescription());
        assertEquals(expected.getCurrencyCode(), actual.getCurrencyCode());
        assertEquals(expected.getIntent(), actual.getIntent());
        assertEquals(expected.getLocaleCode(), actual.getLocaleCode());
        assertEquals(expected.getShippingAddressOverride().toString(), actual.getShippingAddressOverride().toString());
        assertEquals(expected.isShippingAddressRequired(), actual.isShippingAddressRequired());
        assertEquals(expected.getUserAction(), actual.getUserAction());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getLandingPageType(), actual.getLandingPageType());
        assertTrue(actual.shouldOfferCredit());
    }
}
