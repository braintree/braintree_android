package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PayPalTwoFactorAuthRequestUnitTest {

    @Test
    public void toJson_returnsCorrectlyFormattedJsonString() throws JSONException {
       PayPalTwoFactorAuthRequest request = new PayPalTwoFactorAuthRequest()
               .amount("1000")
               .nonce("fake-nonce")
               .currencyCode("INR");

        String fakeAuthFingerprint = "fake-auth-fingerprint";
        String fakeReturnUrlScheme = "fake-url-scheme";
        JSONObject jsonRequest = new JSONObject(request.toJson(fakeAuthFingerprint, fakeReturnUrlScheme));

        assertEquals(jsonRequest.getString("authorization_fingerprint"), "fake-auth-fingerprint");
        assertEquals(jsonRequest.getString("amount"), "1000");
        assertEquals(jsonRequest.getString("currency_iso_code"), "INR");
        assertEquals(jsonRequest.getString("return_url"), "fake-url-scheme://success");
        assertEquals(jsonRequest.getString("cancel_url"), "fake-url-scheme://cancel");
        assertEquals(jsonRequest.getString("vault_initiated_checkout_payment_method_token"), "fake-nonce");
    }
}
