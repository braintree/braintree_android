package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.paypal.PayPalBillingCycle;
import com.braintreepayments.api.paypal.PayPalBillingInterval;
import com.braintreepayments.api.paypal.PayPalBillingPricing;
import com.braintreepayments.api.paypal.PayPalCheckoutRequest;
import com.braintreepayments.api.paypal.PayPalPaymentIntent;
import com.braintreepayments.api.paypal.PayPalPricingModel;
import com.braintreepayments.api.paypal.PayPalRecurringBillingDetails;
import com.braintreepayments.api.paypal.PayPalRecurringBillingPlanType;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalVaultRequest;
import com.braintreepayments.api.core.PostalAddress;

import java.util.List;

public class PayPalRequestFactory {

    public static PayPalVaultRequest createPayPalVaultRequest(
        Context context,
        String buyerEmailAddress
    ) {

        PayPalVaultRequest request = new PayPalVaultRequest(true);

        boolean useAppLink = Settings.getPayPalLinkType(context).equals(context.getString(R.string.paypal_app_link));
        request.setAppLinkEnabled(useAppLink);
        if (!buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        if (Settings.isPayPalCreditOffered(context)) {
            request.setShouldOfferCredit(true);
        }

        if (Settings.usePayPalAddressOverride(context)) {
            PostalAddress postalAddress = new PostalAddress();
            postalAddress.setRecipientName("Brian Tree");
            postalAddress.setStreetAddress("123 Fake Street");
            postalAddress.setExtendedAddress("Floor A");
            postalAddress.setLocality("San Francisco");
            postalAddress.setRegion("CA");
            postalAddress.setCountryCodeAlpha2("US");

            request.setShippingAddressOverride(postalAddress);
        }

//        PayPalBillingInterval billingInterval = PayPalBillingInterval.MONTH;
//        PayPalPricingModel pricingModel = PayPalPricingModel.FIXED;
//        PayPalBillingPricing billingPricing =
//                new PayPalBillingPricing(pricingModel, "1.00");
//        billingPricing.setReloadThresholdAmount("6.00");
//        PayPalBillingCycle billingCycle =
//                new PayPalBillingCycle(billingInterval, 1, 2);
//        billingCycle.setSequence(1);
//        billingCycle.setStartDate("2024-04-06T00:00:00Z");
//        billingCycle.setTrial(true);
//        billingCycle.setPricing(billingPricing);
//        PayPalRecurringBillingDetails billingDetails =
//                new PayPalRecurringBillingDetails(List.of(billingCycle), "USD");
//        billingDetails.setOneTimeFeeAmount("2.00");
//        billingDetails.setProductName("A Product");
//        billingDetails.setProductDescription("A Description");
//        billingDetails.setProductQuantity(1);
//        billingDetails.setShippingAmount("5.00");
//        billingDetails.setTaxAmount("3.00");
//        billingDetails.setTotalAmount("11.00");
//        request.setRecurringBillingDetails(billingDetails);
//        request.setRecurringBillingPlanType(PayPalRecurringBillingPlanType.RECURRING);

        return request;
    }

    public static PayPalCheckoutRequest createPayPalCheckoutRequest(
        Context context,
        String amount,
        String buyerEmailAddress
    ) {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount, true);

        boolean useAppLink = Settings.getPayPalLinkType(context).equals(context.getString(R.string.paypal_app_link));
        request.setAppLinkEnabled(useAppLink);
        if (!buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        String intentType = Settings.getPayPalIntentType(context);
        if (intentType.equals(context.getString(R.string.paypal_intent_authorize))) {
            request.setIntent(PayPalPaymentIntent.AUTHORIZE);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_order))) {
            request.setIntent(PayPalPaymentIntent.ORDER);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_sale))) {
            request.setIntent(PayPalPaymentIntent.SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(context)) {
            request.setUserAction(PayPalCheckoutRequest.USER_ACTION_COMMIT);
        }

        if (Settings.usePayPalAddressOverride(context)) {
            PostalAddress shippingAddress = new PostalAddress();
            shippingAddress.setRecipientName("Brian Tree");
            shippingAddress.setStreetAddress("123 Fake Street");
            shippingAddress.setExtendedAddress("Floor A");
            shippingAddress.setLocality("San Francisco");
            shippingAddress.setRegion("CA");
            shippingAddress.setCountryCodeAlpha2("US");
            shippingAddress.setPostalCode("94103");

            request.setShippingAddressOverride(shippingAddress);
        }

        return request;
    }
}
