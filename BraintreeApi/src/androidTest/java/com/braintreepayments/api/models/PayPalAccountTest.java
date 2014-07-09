package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.api.FixturesHelper;
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
    }

    public void testGetEmailReturnsEmptyStringIfDetailsIsNull() {
        PayPalAccount payPalAccount = new PayPalAccount();
        assertEquals("", payPalAccount.getEmail());
    }

    public void testDescriptionUsesGetEmailIfDescriptionIsPayPalAndEmailIsNotEmpty() {
        PayPalAccount payPalAccount = spy(new PayPalAccount());
        payPalAccount.description = "PayPal";
        when(payPalAccount.getEmail()).thenReturn("test_email_address");

        assertEquals("test_email_address", payPalAccount.getDescription());
    }
}
