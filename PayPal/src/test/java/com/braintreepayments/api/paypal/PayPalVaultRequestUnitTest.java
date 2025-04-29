package com.braintreepayments.api.paypal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import android.os.Build;
import android.os.Parcel;

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RunWith(RobolectricTestRunner.class)
public class PayPalVaultRequestUnitTest {

    @Test
    public void newPayPalVaultRequest_setsDefaultValues() {
        PayPalVaultRequest request = new PayPalVaultRequest(false);

        assertNull(request.getShopperSessionId());
        assertNull(request.getLocaleCode());
        assertFalse(request.isShippingAddressRequired());
        assertNull(request.getShippingAddressOverride());
        assertNull(request.getDisplayName());
        assertNull(request.getLandingPageType());
        assertFalse(request.getShouldOfferCredit());
        assertFalse(request.getHasUserLocationConsent());
        assertFalse(request.getEnablePayPalAppSwitch());
    }

    @Test
    public void setsValuesCorrectly() {
        PostalAddress postalAddress = new PostalAddress();
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setShopperSessionId("shopper-insights-id");
        request.setLocaleCode("US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressOverride(postalAddress);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
        request.setShouldOfferCredit(true);
        PayPalBillingInterval billingInterval = PayPalBillingInterval.MONTH;
        PayPalPricingModel pricingModel = PayPalPricingModel.FIXED;
        PayPalBillingPricing billingPricing =
                new PayPalBillingPricing(pricingModel, "1.00");
        billingPricing.setReloadThresholdAmount("6.00");
        PayPalBillingCycle billingCycle =
                new PayPalBillingCycle(true, 2, billingInterval, 1);
        billingCycle.setSequence(1);
        billingCycle.setStartDate("2024-04-06T00:00:00Z");
        billingCycle.setPricing(billingPricing);
        PayPalRecurringBillingDetails billingDetails =
                new PayPalRecurringBillingDetails(List.of(billingCycle), "11.00", "USD");
        billingDetails.setOneTimeFeeAmount("2.00");
        billingDetails.setProductName("A Product");
        billingDetails.setProductDescription("A Description");
        billingDetails.setProductQuantity(1);
        billingDetails.setShippingAmount("5.00");
        billingDetails.setTaxAmount("3.00");
        request.setRecurringBillingDetails(billingDetails);
        request.setRecurringBillingPlanType(PayPalRecurringBillingPlanType.RECURRING);

        assertEquals("shopper-insights-id", request.getShopperSessionId());
        assertEquals("US", request.getLocaleCode());
        assertEquals("Billing Agreement Description", request.getBillingAgreementDescription());
        assertTrue(request.isShippingAddressRequired());
        assertEquals(postalAddress, request.getShippingAddressOverride());
        assertEquals("Display Name", request.getDisplayName());
        assertEquals("123-correlation", request.getRiskCorrelationId());
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, request.getLandingPageType());
        assertTrue(request.getShouldOfferCredit());
        assertTrue(request.getHasUserLocationConsent());
        assertEquals(PayPalRecurringBillingPlanType.RECURRING, request.getRecurringBillingPlanType());
        assertEquals("USD", request.getRecurringBillingDetails().getCurrencyISOCode());
        assertEquals("2.00", request.getRecurringBillingDetails().getOneTimeFeeAmount());
        assertEquals("A Product", request.getRecurringBillingDetails().getProductName());
        assertEquals("A Description", request.getRecurringBillingDetails().getProductDescription());
        assertSame(1,
                Objects.requireNonNull(request.getRecurringBillingDetails().getProductQuantity()));
        assertEquals("5.00", request.getRecurringBillingDetails().getShippingAmount());
        assertEquals("3.00", request.getRecurringBillingDetails().getTaxAmount());
        assertEquals("11.00", request.getRecurringBillingDetails().getTotalAmount());
        PayPalBillingCycle requestBillingCycle = request.getRecurringBillingDetails().getBillingCycles().get(0);
        assertEquals(PayPalBillingInterval.MONTH, requestBillingCycle.getInterval());
        assertSame(1, requestBillingCycle.getIntervalCount());
        assertEquals(2, requestBillingCycle.getNumberOfExecutions());
        assertEquals("2024-04-06T00:00:00Z", requestBillingCycle.getStartDate());
        assertSame(1, requestBillingCycle.getSequence());
        assertTrue(requestBillingCycle.isTrial());
        PayPalBillingPricing requestBillingPricing = requestBillingCycle.getPricing();
        assertEquals("6.00", requestBillingPricing.getReloadThresholdAmount());
        assertEquals("1.00", requestBillingPricing.getAmount());
        assertEquals(PayPalPricingModel.FIXED, requestBillingPricing.getPricingModel());
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

        request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");
        request.setUserPhoneNumber(new PayPalPhoneNumber("1", "1231231234"));

        PayPalBillingInterval billingInterval = PayPalBillingInterval.MONTH;
        PayPalPricingModel pricingModel = PayPalPricingModel.FIXED;
        PayPalBillingPricing billingPricing =
                new PayPalBillingPricing(pricingModel, "1.00");
        billingPricing.setReloadThresholdAmount("6.00");
        PayPalBillingCycle billingCycle =
                new PayPalBillingCycle(true, 2, billingInterval, 1);
        billingCycle.setSequence(1);
        billingCycle.setStartDate("2024-04-06T00:00:00Z");
        billingCycle.setPricing(billingPricing);

        RecurringBillingAmountBreakdown amountBreakdown = new RecurringBillingAmountBreakdown(
                "10.00",  // shipping
                "8.50",   // taxTotal
                "100.00" // itemTotal
        );

        PayPalRecurringBillingDetails billingDetails = new PayPalRecurringBillingDetails(
                List.of(billingCycle), // billingCycles
                "11.00",               // totalAmount
                "USD",                 // currencyISOCode
                null,                  // productName
                null,                  // oneTimeFeeAmount
                null,                  // productDescription
                null,                  // productAmount
                null,                  // productQuantity
                null,                  // shippingAmount
                null,                  // taxAmount
                "11.00",               // unitAmount
                amountBreakdown        // amountBreakdown
        );

        billingDetails.setOneTimeFeeAmount("2.00");
        billingDetails.setProductName("A Product");
        billingDetails.setProductDescription("A Description");
        billingDetails.setProductQuantity(1);
        billingDetails.setShippingAmount("5.00");
        billingDetails.setTaxAmount("3.00");
        request.setRecurringBillingDetails(billingDetails);
        request.setRecurringBillingPlanType(PayPalRecurringBillingPlanType.RECURRING);

        ArrayList<PayPalLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PayPalLineItem(PayPalLineItemKind.DEBIT, "An Item", "1", "1"));
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
        assertEquals("Postal Address", result.getShippingAddressOverride().getRecipientName());
        assertEquals(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN, result.getLandingPageType());
        assertEquals("Display Name", result.getDisplayName());
        assertEquals("123-correlation", result.getRiskCorrelationId());
        assertEquals("merchant_account_id", result.getMerchantAccountId());
        assertEquals(1, result.getLineItems().size());
        assertEquals("An Item", result.getLineItems().get(0).getName());
        assertEquals("1", result.getUserPhoneNumber().getCountryCode());
        assertEquals("1231231234", result.getUserPhoneNumber().getNationalNumber());
        assertTrue(result.getHasUserLocationConsent());
        assertEquals(PayPalRecurringBillingPlanType.RECURRING, result.getRecurringBillingPlanType());
        assertEquals("USD", result.getRecurringBillingDetails().getCurrencyISOCode());
        assertEquals("2.00", result.getRecurringBillingDetails().getOneTimeFeeAmount());
        assertEquals("A Product", result.getRecurringBillingDetails().getProductName());
        assertEquals("A Description", result.getRecurringBillingDetails().getProductDescription());
        assertSame(1,
                Objects.requireNonNull(result.getRecurringBillingDetails().getProductQuantity()));
        assertEquals("5.00", result.getRecurringBillingDetails().getShippingAmount());
        assertEquals("3.00", result.getRecurringBillingDetails().getTaxAmount());
        assertEquals("11.00", result.getRecurringBillingDetails().getTotalAmount());
        PayPalBillingCycle resultBillingCycle = result.getRecurringBillingDetails().getBillingCycles().get(0);
        assertEquals(PayPalBillingInterval.MONTH, resultBillingCycle.getInterval());
        assertSame(1, resultBillingCycle.getIntervalCount());
        assertEquals(2, resultBillingCycle.getNumberOfExecutions());
        assertEquals("2024-04-06T00:00:00Z", resultBillingCycle.getStartDate());
        assertSame(1, resultBillingCycle.getSequence());
        assertTrue(resultBillingCycle.isTrial());
        PayPalBillingPricing resultBillingPricing = resultBillingCycle.getPricing();
        assertEquals("6.00", resultBillingPricing.getReloadThresholdAmount());
        assertEquals("1.00", resultBillingPricing.getAmount());
        assertEquals(PayPalPricingModel.FIXED, resultBillingPricing.getPricingModel());

        assertEquals("100.00", billingDetails.getAmountBreakdown().getItemTotal());
        assertEquals("10.00", billingDetails.getAmountBreakdown().getShipping());
        assertEquals("8.50", billingDetails.getAmountBreakdown().getTaxTotal());
    }

    @Test
    public void createRequestBody_sets_userAuthenticationEmail_when_not_null() throws JSONException {
        String payerEmail = "payer_email@example.com";
        PayPalVaultRequest request = new PayPalVaultRequest(true);

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
    public void createRequestBody_sets_enablePayPalSwitch_and_userAuthenticationEmail_not_null() throws JSONException {
        String versionSDK = String.valueOf(Build.VERSION.SDK_INT);
        String payerEmail = "payer_email@example.com";
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setEnablePayPalAppSwitch(true);
        request.setUserAuthenticationEmail(payerEmail);
        String requestBody = request.createRequestBody(
            mock(Configuration.class),
            mock(Authorization.class),
            "success_url",
            "cancel_url",
            "universal_url"
        );

        assertTrue(requestBody.contains("\"launch_paypal_app\":true"));
        assertTrue(requestBody.contains("\"os_type\":" + "\"Android\""));
        assertTrue(requestBody.contains("\"os_version\":" + "\"" + versionSDK + "\""));
        assertTrue(requestBody.contains("\"merchant_app_return_url\":" + "\"universal_url\""));
    }

    @Test
    public void createRequestBody_sets_shopper_insights_session_id() throws JSONException {
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setShopperSessionId("shopper-insights-id");
        String requestBody = request.createRequestBody(
                mock(Configuration.class),
                mock(Authorization.class),
                "success_url",
                "cancel_url",
                "universal_url"
        );

        assertTrue(requestBody.contains("\"shopper_session_id\":" + "\"shopper-insights-id\""));
    }

    @Test
    public void createRequestBody_correctlyFormatsJSON() throws JSONException {
        PayPalVaultRequest request = new PayPalVaultRequest(true);
        request.setLocaleCode("en-US");
        request.setBillingAgreementDescription("Billing Agreement Description");
        request.setShippingAddressRequired(true);
        request.setShippingAddressEditable(true);
        request.setShouldOfferCredit(true);
        request.setUserAuthenticationEmail("email");

        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setRecipientName("Postal Address");
        request.setShippingAddressOverride(postalAddress);

        request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
        request.setDisplayName("Display Name");
        request.setRiskCorrelationId("123-correlation");
        request.setMerchantAccountId("merchant_account_id");

        PayPalBillingInterval billingInterval = PayPalBillingInterval.MONTH;
        PayPalPricingModel pricingModel = PayPalPricingModel.VARIABLE;
        PayPalBillingPricing billingPricing =
                new PayPalBillingPricing(pricingModel, "1.00");
        billingPricing.setReloadThresholdAmount("6.00");
        PayPalBillingCycle billingCycle =
                new PayPalBillingCycle(true, 2, billingInterval, 1);
        billingCycle.setSequence(1);
        billingCycle.setStartDate("2024-04-06T00:00:00Z");
        billingCycle.setPricing(billingPricing);
        PayPalRecurringBillingDetails billingDetails =
                new PayPalRecurringBillingDetails(List.of(billingCycle), "11.00", "USD");
        billingDetails.setOneTimeFeeAmount("2.00");
        billingDetails.setProductName("A Product");
        billingDetails.setProductDescription("A Description");
        billingDetails.setProductQuantity(1);
        billingDetails.setShippingAmount("5.00");
        billingDetails.setTaxAmount("3.00");
        request.setRecurringBillingDetails(billingDetails);
        request.setRecurringBillingPlanType(PayPalRecurringBillingPlanType.RECURRING);

        String requestBody = request.createRequestBody(
                mock(Configuration.class),
                mock(Authorization.class),
                "success_url",
                "cancel_url",
                null
        );

        JSONAssert.assertEquals(Fixtures.PAYPAL_REQUEST_JSON, requestBody, false);
    }

    @Test
    public void createRequestBody_sets_userPhoneNumber_when_not_null() throws JSONException {
        PayPalVaultRequest request = new PayPalVaultRequest(true);

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
