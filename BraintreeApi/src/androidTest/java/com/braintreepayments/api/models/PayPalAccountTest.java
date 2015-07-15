package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.TestUtils;
import com.braintreepayments.testutils.FixturesHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class PayPalAccountTest {

    @Before
    public void setUp() {
        TestUtils.setUp(getTargetContext());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void payPalAccountTypeIsPayPal() {
        assertEquals("PayPal", new PayPalAccount().getTypeLabel());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesResponse() throws JSONException {
        String paypalString = FixturesHelper.stringFromFixture(getTargetContext(),
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

    @Test(timeout = 1000)
    @SmallTest
    public void getDescription_usesGetEmailIfDescriptionIsPayPalAndEmailIsNotEmpty() {
        PayPalAccount payPalAccount = spy(new PayPalAccount());
        payPalAccount.mDescription = "PayPal";
        when(payPalAccount.getEmail()).thenReturn("test_email_address");

        assertEquals("test_email_address", payPalAccount.getDescription());
    }
}