package com.braintreepayments.api.paypal;

import android.net.Uri;
import android.os.Parcel;

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.PostalAddress;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PayPalCheckoutRequestUnitTest {

    @Test
    public void newPayPalCheckoutRequest_setsDefaultValues() {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", false);

        assertNotNull(request.getAmount());
        assertNull(request.getCurrencyCode());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertEquals(PayPalPaymentIntent.AUTHORIZE, request.getIntent());
        assertNull(request.getLandingPageType());
        assertNull(request.getBillingAgreementDescription());
        assertFalse(request.getShouldOfferPayLater());
        assertFalse(request.getHasUserLocationConsent());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);
        request.setCurrencyCode("USD");
        request.setShouldOfferPayLater(true);
        request.setIntent(PayPalPaymentIntent.SALE);

        request.setLocaleCode("US");
        request.setShouldRequestBillingAgreement(true);
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setUserAction(PayPalPaymentUserAction.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);

        assertEquals("1.00", request.getAmount());
        assertEquals("USD", request.getCurrencyCode());
        assertEquals("US", request.getLocaleCode());
        assertTrue(request.getShouldRequestBillingAgreement());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals(PayPalPaymentIntent.SALE, request.getIntent());
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, request.getUserAction());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals("123-correlation", request.getRiskCorrelationId());
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.getShouldOfferPayLater());
        assertTrue(request.getHasUserLocationConsent());
    }

    @Test
    public void parcelsCorrectly() {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("12.34", true);
        request.setCurrencyCode("USD");
        request.setLocaleCode("en-US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressEditable(true);

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setRecipientName("Postal Address");
        request.setShippingAddressOverride(postalAddress);

        request.setIntent(PayPalPaymentIntent.SALE);
        request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
        request.setUserAction(PayPalPaymentUserAction.USER_ACTION_COMMIT);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");

        ArrayList<PayPalLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PayPalLineItem(PayPalLineItemKind.DEBIT, "An Item", "1", "1"));
        request.setLineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalCheckoutRequest result = PayPalCheckoutRequest.CREATOR.createFromParcel(parcel);

        assertEquals("12.34", result.getAmount());
        assertEquals("USD", result.getCurrencyCode());
        assertEquals("en-US", result.getLocaleCode());
        assertEquals("Billing Agreement Description",
            result.getBillingAgreementDescription());
        assertTrue(result.isShippingAddressRequired());
        assertTrue(result.isShippingAddressEditable());
        assertEquals("Postal Address", result.getShippingAddressOverride().getRecipientName());
        assertEquals(PayPalPaymentIntent.SALE, result.getIntent());
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, result.getLandingPageType());
        assertEquals(PayPalPaymentUserAction.USER_ACTION_COMMIT, result.getUserAction());
        assertEquals("Display Name", result.getDisplayName());
        assertEquals("123-correlation", result.getRiskCorrelationId());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
        assertTrue(result.getHasUserLocationConsent());
    }

    @Test
    public void createRequestBody_sets_userAuthenticationEmail_when_not_null() throws JSONException {
        String payerEmail = "payer_email@example.com";
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);

        request.setUserAuthenticationEmail(payerEmail);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url",
            null
        );

        assertTrue(requestBody.contains("\"payer_email\":" + "\"" + payerEmail + "\""));
    }

    @Test
    public void createRequestBody_does_not_set_userAuthenticationEmail_when_email_is_empty() throws JSONException {
        String payerEmail = "";
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);

        request.setUserAuthenticationEmail(payerEmail);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url",
            null
        );

        assertFalse(requestBody.contains("\"payer_email\":" + "\"" + payerEmail + "\""));
    }

    @Test
    public void createRequestBody_sets_shippingCallbackUri_when_not_null() throws JSONException {
        String urlString = "https://www.example.com/path";
        Uri uri = Uri.parse(urlString);

        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);
        request.setShippingCallbackUrl(uri);

        String requestBody = request.createRequestBody(
                mock(Configuration.class),
                mock(Authorization.class),
                "success_url",
                "cancel_url",
                null
        );

        JSONObject jsonObject = new JSONObject(requestBody);
        assertEquals(urlString, jsonObject.getString("shipping_callback_url"));
    }

    @Test
    public void createRequestBody_does_not_set_shippingCallbackUri_when_null() throws JSONException {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);

        String requestBody = request.createRequestBody(
                mock(Configuration.class),
                mock(Authorization.class),
                "success_url",
                "cancel_url",
                null
        );

        JSONObject jsonObject = new JSONObject(requestBody);
        assertFalse(jsonObject.has("shipping_callback_url"));
    }

    @Test
    public void createRequestBody_does_not_set_shippingCallbackUri_when_empty() throws JSONException {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);
        request.setShippingCallbackUrl(Uri.parse(""));

        String requestBody = request.createRequestBody(
                mock(Configuration.class),
                mock(Authorization.class),
                "success_url",
                "cancel_url",
                null
        );

        JSONObject jsonObject = new JSONObject(requestBody);
        assertFalse(jsonObject.has("shipping_callback_url"));
    }

    public void createRequestBody_sets_userPhoneNumber_when_not_null() throws JSONException {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00", true);

        request.setUserPhoneNumber(new PayPalPhoneNumber("1", "1231231234"));
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url",
            null
        );

        assertTrue(requestBody.contains("\"phone_number\":{\"country_code\":\"1\",\"national_number\":\"1231231234\"}"));
    }
}

