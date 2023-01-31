package com.braintreepayments.api;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PayPalCheckoutRequestUnitTest {

    @Test
    public void newPayPalCheckoutRequest_setsDefaultValues() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.00");

        assertNotNull(request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getDisplayName());
        assertEquals(PayPalNativeCheckoutPaymentIntent.AUTHORIZE, request.getIntent());
        assertNull(request.getBillingAgreementDescription());
        assertFalse(request.getShouldOfferPayLater());
    }

    @Test
    public void setsValuesCorrectly() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.00");
        request.setCurrencyCode("USD");
        request.setShouldOfferPayLater(true);
        request.setIntent(PayPalNativeCheckoutPaymentIntent.SALE);

        request.setLocaleCode("US");
        request.setShouldRequestBillingAgreement(true);
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setUserAction(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");

        PostalAddress shippingAddress = new PostalAddress();
        shippingAddress.setRecipientName("Brian Tree");
        shippingAddress.setStreetAddress("123 Fake Street");
        shippingAddress.setExtendedAddress("Floor A");
        shippingAddress.setPostalCode("94103");
        shippingAddress.setLocality("San Francisco");
        shippingAddress.setRegion("CA");
        shippingAddress.setCountryCodeAlpha2("US");

        request.setShippingAddressOverride(shippingAddress);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertTrue(request.getShouldRequestBillingAgreement());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(PayPalNativeCheckoutPaymentIntent.SALE, request.getIntent());
        assertEquals(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals("Brian Tree", request.getShippingAddressOverride().getRecipientName());
        assertEquals("123 Fake Street", request.getShippingAddressOverride().getStreetAddress());
        assertEquals("Floor A", request.getShippingAddressOverride().getExtendedAddress());
        assertEquals("94103", request.getShippingAddressOverride().getPostalCode());
        assertEquals("San Francisco", request.getShippingAddressOverride().getLocality());
        assertEquals("CA", request.getShippingAddressOverride().getRegion());
        assertEquals("US", request.getShippingAddressOverride().getCountryCodeAlpha2());
        assertTrue(request.getShouldOfferPayLater());
    }

    @Test
    public void parcelsCorrectly() {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("12.34");
        request.setCurrencyCode("USD");
        request.setLocaleCode("en-US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressEditable(true);

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setRecipientName("Postal Address");
        request.setShippingAddressOverride(postalAddress);

        request.setIntent(PayPalNativeCheckoutPaymentIntent.SALE);
        request.setUserAction(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");

        ArrayList<PayPalNativeCheckoutLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PayPalNativeCheckoutLineItem(PayPalNativeCheckoutLineItem.KIND_DEBIT, "An Item", "1", "1"));
        request.setLineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalNativeCheckoutRequest result = PayPalNativeCheckoutRequest.CREATOR.createFromParcel(parcel);

        assertEquals("12.34", result.getAmount());
        assertEquals("USD", result.getCurrencyCode());
        assertEquals("en-US", result.getLocaleCode());
        assertEquals("Billing Agreement Description",
                result.getBillingAgreementDescription());
        assertTrue(result.isShippingAddressRequired());
        assertEquals(PayPalNativeCheckoutPaymentIntent.SALE, result.getIntent());
        assertEquals(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT, result.getUserAction());
        assertEquals("Display Name", result.getDisplayName());
        assertEquals("123-correlation", result.getRiskCorrelationId());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
    }
}
