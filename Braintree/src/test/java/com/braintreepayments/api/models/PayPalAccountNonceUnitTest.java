package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PayPalAccountNonceUnitTest {

    @Test
    public void payPalAccountTypeIsPayPal() {
        assertEquals("PayPal", new PayPalAccountNonce().getTypeLabel());
    }

    @Test
    public void fromJson_parsesResponseWithCreditFinancingOffer() throws JSONException {
        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(
                stringFromFixture("payment_methods/paypal_account_response.json"));

        assertNotNull(payPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", payPalAccountNonce.getDescription());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.getNonce());
        assertEquals("paypalaccount@example.com", payPalAccountNonce.getEmail());
        assertEquals("PayPal", payPalAccountNonce.getTypeLabel());
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
        JSONObject response = new JSONObject(stringFromFixture("payment_methods/paypal_account_response.json"));
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").remove("creditFinancingOffered");
        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(response.toString());

        assertNotNull(payPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", payPalAccountNonce.getDescription());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payPalAccountNonce.getNonce());
        assertEquals("paypalaccount@example.com", payPalAccountNonce.getEmail());
        assertEquals("PayPal", payPalAccountNonce.getTypeLabel());
        assertEquals("123 Fake St.", payPalAccountNonce.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", payPalAccountNonce.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", payPalAccountNonce.getBillingAddress().getLocality());
        assertEquals("CA", payPalAccountNonce.getBillingAddress().getRegion());
        assertEquals("94602", payPalAccountNonce.getBillingAddress().getPostalCode());
        assertEquals("US", payPalAccountNonce.getBillingAddress().getCountryCodeAlpha2());

        assertNull(payPalAccountNonce.getCreditFinancing());
    }

    @Test
    public void getDescription_usesGetEmailIfDescriptionIsPayPalAndEmailIsNotEmpty() {
        PayPalAccountNonce payPalAccountNonce = spy(new PayPalAccountNonce());
        payPalAccountNonce.mDescription = "PayPal";
        when(payPalAccountNonce.getEmail()).thenReturn("test_email_address");

        assertEquals("test_email_address", payPalAccountNonce.getDescription());
    }

    @Test
    public void parcelsCorrectly_withAllValuesPresent() throws JSONException {
        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(
                stringFromFixture("payment_methods/paypal_account_response.json"));
        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalAccountNonce parceled = PayPalAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals("with email paypalaccount@example.com", parceled.getDescription());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.getNonce());
        assertEquals("paypalaccount@example.com", parceled.getEmail());
        assertEquals("PayPal", parceled.getTypeLabel());
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
        JSONObject response = new JSONObject(stringFromFixture("payment_methods/paypal_account_response.json"));
        response.getJSONArray("paypalAccounts").getJSONObject(0).getJSONObject("details").remove("creditFinancingOffered");
        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(response.toString());
        assertNull(payPalAccountNonce.getCreditFinancing());

        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalAccountNonce parceled = PayPalAccountNonce.CREATOR.createFromParcel(parcel);

        assertNull(parceled.getCreditFinancing());

        assertEquals("with email paypalaccount@example.com", parceled.getDescription());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", parceled.getNonce());
        assertEquals("paypalaccount@example.com", parceled.getEmail());
        assertEquals("PayPal", parceled.getTypeLabel());
        assertEquals("123 Fake St.", parceled.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", parceled.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", parceled.getBillingAddress().getLocality());
        assertEquals("CA", parceled.getBillingAddress().getRegion());
        assertEquals("94602", parceled.getBillingAddress().getPostalCode());
        assertEquals("US", parceled.getBillingAddress().getCountryCodeAlpha2());
    }
}