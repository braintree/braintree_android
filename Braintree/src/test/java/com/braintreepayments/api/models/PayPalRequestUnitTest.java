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
        assertEquals(PayPalRequest.INTENT_AUTHORIZE, request.getIntent());
        assertNull(request.getLandingPageType());
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
                .landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalRequest.INTENT_SALE, request.getIntent());
        assertEquals(PayPalRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
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
                .landingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);

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
        assertEquals(expected.getLandingPageType(), actual.getLandingPageType());
    }
}
