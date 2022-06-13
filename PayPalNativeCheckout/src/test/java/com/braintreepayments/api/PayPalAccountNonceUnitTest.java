package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalAccountNonceUnitTest {

    @Test
    public void fromJson_parsesResponseWithCreditFinancingOffer() throws JSONException {
        PayPalNativeCheckoutAccountNonce payPalAccountNonce = PayPalNativeCheckoutAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE));

        assertNotNull(payPalAccountNonce);
        assertEquals("fake-authenticate-url", payPalAccountNonce.getAuthenticateUrl());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.getString());
        assertEquals("paypalaccount@example.com", payPalAccountNonce.getEmail());
        assertEquals("123 Fake St.", payPalAccountNonce.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", payPalAccountNonce.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", payPalAccountNonce.getBillingAddress().getLocality());
        assertEquals("CA", payPalAccountNonce.getBillingAddress().getRegion());
        assertEquals("94602", payPalAccountNonce.getBillingAddress().getPostalCode());
        assertEquals("US", payPalAccountNonce.getBillingAddress().getCountryCodeAlpha2());

        assertFalse(payPalAccountNonce.getCreditFinancing().isCardAmountImmutable());
        assertEquals("USD", payPalAccountNonce.getCreditFinancing().getMonthlyPayment().getCurrency());
        assertEquals("13.88", payPalAccountNonce.getCreditFinancing().getMonthlyPayment().getValue());
        assertTrue(payPalAccountNonce.getCreditFinancing().hasPayerAcceptance());
        assertEquals(18, payPalAccountNonce.getCreditFinancing().getTerm());
        assertEquals("USD", payPalAccountNonce.getCreditFinancing().getTotalCost().getCurrency());
        assertEquals("250.00", payPalAccountNonce.getCreditFinancing().getTotalCost().getValue());
        assertEquals("USD", payPalAccountNonce.getCreditFinancing().getTotalInterest().getCurrency());
        assertEquals("0.00", payPalAccountNonce.getCreditFinancing().getTotalInterest().getValue());
    }

    @Test
    public void fromJson_parsesResponseWithoutCreditFinancingOffer() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE);
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").remove("creditFinancingOffered");
        PayPalNativeCheckoutAccountNonce payPalAccountNonce = PayPalNativeCheckoutAccountNonce.fromJSON(response);

        assertNotNull(payPalAccountNonce);
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.getString());
        assertEquals("paypalaccount@example.com", payPalAccountNonce.getEmail());
        assertEquals("123 Fake St.", payPalAccountNonce.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", payPalAccountNonce.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", payPalAccountNonce.getBillingAddress().getLocality());
        assertEquals("CA", payPalAccountNonce.getBillingAddress().getRegion());
        assertEquals("94602", payPalAccountNonce.getBillingAddress().getPostalCode());
        assertEquals("US", payPalAccountNonce.getBillingAddress().getCountryCodeAlpha2());

        assertNull(payPalAccountNonce.getCreditFinancing());
    }

    @Test
    public void fromJson_whenNoAddresses_returnsEmptyPostalAddress() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_ADDRESSES);
        PayPalNativeCheckoutAccountNonce paypalAccount = PayPalNativeCheckoutAccountNonce.fromJSON(response);

        assertNotNull(paypalAccount.getShippingAddress());
        assertNotNull(paypalAccount.getBillingAddress());
    }

    @Test
    public void parcelsCorrectly_withAllValuesPresent() throws JSONException {
        PayPalNativeCheckoutAccountNonce payPalAccountNonce = PayPalNativeCheckoutAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE));
        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalNativeCheckoutAccountNonce parceled = PayPalNativeCheckoutAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.getString());
        assertEquals("paypalaccount@example.com", parceled.getEmail());
        assertEquals("fake-authenticate-url", parceled.getAuthenticateUrl());

        assertEquals("123 Fake St.", parceled.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", parceled.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", parceled.getBillingAddress().getLocality());
        assertEquals("CA", parceled.getBillingAddress().getRegion());
        assertEquals("94602", parceled.getBillingAddress().getPostalCode());
        assertEquals("US", parceled.getBillingAddress().getCountryCodeAlpha2());

        assertFalse(parceled.getCreditFinancing().isCardAmountImmutable());
        assertEquals("USD", parceled.getCreditFinancing().getMonthlyPayment().getCurrency());
        assertEquals("13.88", parceled.getCreditFinancing().getMonthlyPayment().getValue());
        assertTrue(parceled.getCreditFinancing().hasPayerAcceptance());
        assertEquals(18, parceled.getCreditFinancing().getTerm());
        assertEquals("USD", parceled.getCreditFinancing().getTotalCost().getCurrency());
        assertEquals("250.00", parceled.getCreditFinancing().getTotalCost().getValue());
        assertEquals("USD", parceled.getCreditFinancing().getTotalInterest().getCurrency());
        assertEquals("0.00", parceled.getCreditFinancing().getTotalInterest().getValue());
    }

    @Test
    public void parcelsCorrectly_ifCreditFinancingNotPresent() throws JSONException {
        JSONObject response = new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE);
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").remove("creditFinancingOffered");
        PayPalNativeCheckoutAccountNonce payPalAccountNonce = PayPalNativeCheckoutAccountNonce.fromJSON(response);
        assertNull(payPalAccountNonce.getCreditFinancing());

        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalNativeCheckoutAccountNonce parceled = PayPalNativeCheckoutAccountNonce.CREATOR.createFromParcel(parcel);

        assertNull(parceled.getCreditFinancing());

        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.getString());
        assertEquals("paypalaccount@example.com", parceled.getEmail());
        assertEquals("123 Fake St.", parceled.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", parceled.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", parceled.getBillingAddress().getLocality());
        assertEquals("CA", parceled.getBillingAddress().getRegion());
        assertEquals("94602", parceled.getBillingAddress().getPostalCode());
        assertEquals("US", parceled.getBillingAddress().getCountryCodeAlpha2());
    }
}