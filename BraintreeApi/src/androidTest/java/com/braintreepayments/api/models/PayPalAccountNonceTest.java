package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PayPalAccountNonceTest {

    @Test(timeout = 1000)
    public void payPalAccountTypeIsPayPal() {
        assertEquals("PayPal", new PayPalAccountNonce().getTypeLabel());
    }

    @Test(timeout = 1000)
    public void fromJson_parsesResponse() throws JSONException {
        String paypalString = stringFromFixture("payment_methods/paypal_account_response.json");

        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(paypalString);

        assertEquals("with email paypalaccount@example.com", payPalAccountNonce.getDescription());
        assertEquals("aaaaaa-bbbbbbb-109934023-1", payPalAccountNonce.getNonce());
        assertEquals("paypalaccount@example.com", payPalAccountNonce.getEmail());
        assertEquals("PayPal", payPalAccountNonce.getTypeLabel());
        assertEquals("123 Fake St.", payPalAccountNonce.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", payPalAccountNonce.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", payPalAccountNonce.getBillingAddress().getLocality());
        assertEquals("CA", payPalAccountNonce.getBillingAddress().getRegion());
        assertEquals("94602", payPalAccountNonce.getBillingAddress().getPostalCode());
        assertEquals("US", payPalAccountNonce.getBillingAddress().getCountryCodeAlpha2());
    }

    @Test(timeout = 1000)
    public void getDescription_usesGetEmailIfDescriptionIsPayPalAndEmailIsNotEmpty() {
        PayPalAccountNonce payPalAccountNonce = spy(new PayPalAccountNonce());
        payPalAccountNonce.mDescription = "PayPal";
        when(payPalAccountNonce.getEmail()).thenReturn("test_email_address");

        assertEquals("test_email_address", payPalAccountNonce.getDescription());
    }

    @Test(timeout = 1000)
    public void parcelsCorrectly() throws JSONException {
        String paypalString = stringFromFixture("payment_methods/paypal_account_response.json");
        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJson(paypalString);
        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalAccountNonce parceled = PayPalAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals("with email paypalaccount@example.com", parceled.getDescription());
        assertEquals("aaaaaa-bbbbbbb-109934023-1", parceled.getNonce());
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