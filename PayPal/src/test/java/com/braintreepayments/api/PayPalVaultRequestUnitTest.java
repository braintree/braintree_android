package com.braintreepayments.api;

import android.content.pm.Signature;
import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PayPalVaultRequestUnitTest {

    @Test
    public void newPayPalVaultRequest_setsDefaultValues() {
        PayPalVaultRequest request = new PayPalVaultRequest(false);

        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertNull(request.getLandingPageType());
        assertFalse(request.getShouldOfferCredit());
        assertFalse(request.hasUserLocationConsent());
    }

    @Test
    public void newPayPalVaultRequest_without_hasUserLocationConsent_defaults_to_false() {
        PayPalVaultRequest request = new PayPalVaultRequest();
        assertFalse(request.hasUserLocationConsent());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setLocaleCode("US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        request.setShouldOfferCredit(true);

        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals("123-correlation", request.getRiskCorrelationId());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.getShouldOfferCredit());
        assertTrue(request.hasUserLocationConsent());
    }

    @Test
    public void parcelsCorrectly() {
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setLocaleCode("en-US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressEditable(true);
        request.setShouldOfferCredit(true);

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setRecipientName("Postal Address");
        request.setShippingAddressOverride(postalAddress);

        request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");

        ArrayList<PayPalLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "1"));
        request.setLineItems(lineItems);

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PayPalVaultRequest result = PayPalVaultRequest.CREATOR.createFromParcel(parcel);

        assertEquals("en-US", result.getLocaleCode());
        assertEquals("Billing Agreement Description",
            result.getBillingAgreementDescription());
        assertTrue(result.getShouldOfferCredit());
        assertTrue(result.isShippingAddressRequired());
        assertTrue(result.isShippingAddressEditable());
        assertEquals("Postal Address", result.getShippingAddressOverride()
            .getRecipientName());
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, result.getLandingPageType());
        assertEquals("Display Name", result.getDisplayName());
        assertEquals("123-correlation", result.getRiskCorrelationId());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
        assertTrue(result.hasUserLocationConsent());
    }

    @Test
    public void createRequestBody_sets_userAuthenticationEmail_when_not_null() throws JSONException {
        String payerEmail = "payer_email@example.com";
        PayPalVaultRequest request = new PayPalVaultRequest();

        request.setUserAuthenticationEmail(payerEmail);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url"
        );

        assertTrue(requestBody.contains("\"payer_email\":" + "\"" + payerEmail + "\""));
    }

    @Test
    public void createRequestBody_sets_userPhoneNumber_when_not_null() throws JSONException {
        String userPhoneNumber = "1231231234";
        PayPalVaultRequest request = new PayPalVaultRequest(true);

        request.setUserPhoneNumber(userPhoneNumber);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url"
        );

        assertTrue(requestBody.contains("\"phone\":" + "\"" + userPhoneNumber + "\""));
    }

    @Test
    public void createRequestBody_does_not_set_userPhoneNumber_when_email_is_empty() throws JSONException {
        String userPhoneNumber = "";
        PayPalVaultRequest request = new PayPalVaultRequest(true);

        request.setUserPhoneNumber(userPhoneNumber);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url"
        );

        assertFalse(requestBody.contains("\"phone\":" + "\"" + userPhoneNumber + "\""));
    }
}
