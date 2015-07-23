package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;
import com.braintreepayments.api.TestUtils;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PayPalAccountTest extends AndroidTestCase {

    @Override
    public void setUp() {
        TestUtils.setUp(getContext());
    }

    public void testPayPalAccountTypeIsPayPal() {
        assertEquals("PayPal", new PayPalAccount().getTypeLabel());
    }

    public void testPayPalParsesFromJson() {
        String paypalString = FixturesHelper.stringFromFixture(getContext(),
                "payment_methods/paypal.json");
        PayPalAccount payPalAccount = PayPalAccount.fromJson(paypalString);

        assertEquals("with email paypalaccount@example.com", payPalAccount.getDescription());
        assertEquals("aaaaaa-bbbbbbb-109934023-1", payPalAccount.getNonce());
        assertEquals("paypalaccount@example.com", payPalAccount.getEmail());
        assertEquals("PayPal", payPalAccount.getTypeLabel());
        assertEquals("123 Fake St.", payPalAccount.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", payPalAccount.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", payPalAccount.getBillingAddress().getLocality());
        assertEquals("CA", payPalAccount.getBillingAddress().getRegion());
        assertEquals("94602", payPalAccount.getBillingAddress().getPostalCode());
        assertEquals("US", payPalAccount.getBillingAddress().getCountryCodeAlpha2());
    }

    public void testPayPalParsesEmailNestedInDetailsFromJson() {
        String paypalString = FixturesHelper.stringFromFixture(getContext(),
                "payment_methods/paypal.json");
        PayPalAccount payPalAccount = PayPalAccount.fromJson(paypalString);
        assertEquals("paypalaccount@example.com", payPalAccount.getEmail());
    }

    public void testPayPalParsesEmailNestedInPayerInfoFromJson() {
        String paypalString = FixturesHelper.stringFromFixture(getContext(),
                "payment_methods/paypal_email_nested_in_payer_info.json");
        PayPalAccount payPalAccount = PayPalAccount.fromJson(paypalString);
        assertEquals("paypal_email_nested_in_payer_info@example.com", payPalAccount.getEmail());
    }

    public void testGetEmailReturnsEmptyStringIfDetailsIsNull() {
        PayPalAccount payPalAccount = new PayPalAccount();
        assertEquals("", payPalAccount.getEmail());
    }

    public void testDescriptionUsesGetEmailIfDescriptionIsPayPalAndEmailIsNotEmpty() {
        PayPalAccount payPalAccount = spy(new PayPalAccount());
        payPalAccount.mDescription = "PayPal";
        when(payPalAccount.getEmail()).thenReturn("test_email_address");

        assertEquals("test_email_address", payPalAccount.getDescription());
    }
}