package com.braintreepayments.api.models;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentRequestUnitTest {

    @Test
    public void build_setsAllParams() throws JSONException {
        PostalAddress address = new PostalAddress()
                .streetAddress("836486 of 22321 Park Lake")
                .extendedAddress("Apt 2")
                .countryCodeAlpha2("NL")
                .locality("Den Haag")
                .region("CA")
                .postalCode("2585 GJ");
        LocalPaymentRequest request = new LocalPaymentRequest()
                .paymentType("ideal")
                .amount("1.10")
                .address(address)
                .phone("639847934")
                .email("jon@getbraintree.com")
                .givenName("Jon")
                .surname("Doe")
                .shippingAddressRequired(false)
                .merchantAccountId("local-merchant-account-id")
                .currencyCode("EUR")
                .paymentTypeCountryCode("NL")
                .bic("bank-id-code");

        JSONObject json = new JSONObject(request.build("http://success-url.com", "http://cancel-url.com"));

        assertEquals("sale", json.getString("intent"));
        assertEquals("1.10", json.getString("amount"));

        assertEquals("sale", json.getString("intent"));
        assertEquals("Jon", json.getString("firstName"));
        assertEquals("Doe", json.getString("lastName"));
        assertEquals("639847934", json.getString("phone"));
        assertEquals("EUR", json.getString("currencyIsoCode"));
        assertEquals("ideal", json.getString("fundingSource"));
        assertEquals("jon@getbraintree.com", json.getString("payerEmail"));
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"));
        assertEquals("CA", json.getString("state"));
        assertEquals("Apt 2", json.getString("line2"));
        assertEquals("Den Haag", json.getString("city"));
        assertEquals("2585 GJ", json.getString("postalCode"));
        assertEquals("NL", json.getString("countryCode"));
        assertEquals("local-merchant-account-id", json.getString("merchantAccountId"));
        assertEquals("NL", json.getString("paymentTypeCountryCode"));
        assertEquals("bank-id-code", json.getString("bic"));
        assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"));
        String expectedCancelUrl = Uri.parse("http://cancel-url.com").toString();
        String expectedReturnUrl = Uri.parse("http://success-url.com").toString();
        assertEquals(expectedCancelUrl, json.getString("cancelUrl"));
        assertEquals(expectedReturnUrl, json.getString("returnUrl"));
    }

    @Test
    public void setsApprovalUrl_andPaymentId_once() {

        LocalPaymentRequest request = new LocalPaymentRequest()
                .paymentId("pid")
                .approvalUrl("aUrl");

        assertEquals("aUrl", request.getApprovalUrl());
        assertEquals("pid", request.getPaymentId());

        request.paymentId("differentPaymentId");
        request.approvalUrl("differentUrl");

        assertEquals("aUrl", request.getApprovalUrl());
        assertEquals("pid", request.getPaymentId());
    }
}

