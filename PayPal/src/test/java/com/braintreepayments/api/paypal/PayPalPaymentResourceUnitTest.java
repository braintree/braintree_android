package com.braintreepayments.api.paypal;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

import com.braintreepayments.api.paypal.PayPalPaymentResource;

public class PayPalPaymentResourceUnitTest {

    @Test
    public void fromJson_parsesRedirectUrlFromOneTimePaymentResource() throws JSONException {
        String oneTimePaymentJson = new JSONObject()
                .put("paymentResource", new JSONObject()
                        .put("redirectUrl", "www.example.com/redirect")
                ).toString();

        PayPalPaymentResource sut = PayPalPaymentResource.fromJson(oneTimePaymentJson);
        assertEquals("www.example.com/redirect", sut.getRedirectUrl());
    }

    @Test
    public void fromJson_parsesRedirectUrlFromBillingAgreementPaymentResource() throws JSONException {
        String billingAgreementJson = new JSONObject()
                .put("agreementSetup", new JSONObject()
                        .put("approvalUrl", "www.example.com/redirect")
                ).toString();

        PayPalPaymentResource sut = PayPalPaymentResource.fromJson(billingAgreementJson);
        assertEquals("www.example.com/redirect", sut.getRedirectUrl());
    }

    @Test
    public void fromJson_parsesRedirectUrlFromBillingAgreementPaymentResource_returnsPayPalRedirectUrl() throws JSONException {
        String billingAgreementJson = new JSONObject()
                .put("agreementSetup", new JSONObject()
                        .put("approvalUrl", "www.example.com/redirect")
                        .put("paypalAppApprovalUrl", "www.paypal.example.com/redirect")
                ).toString();

        PayPalPaymentResource sut = PayPalPaymentResource.fromJson(billingAgreementJson);
        assertEquals("www.paypal.example.com/redirect", sut.getRedirectUrl());
    }
}