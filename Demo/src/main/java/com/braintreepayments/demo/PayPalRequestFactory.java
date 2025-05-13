package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.paypal.PayPalBillingCycle;
import com.braintreepayments.api.paypal.PayPalBillingInterval;
import com.braintreepayments.api.paypal.PayPalBillingPricing;
import com.braintreepayments.api.paypal.PayPalCheckoutRequest;
import com.braintreepayments.api.paypal.PayPalContactInformation;
import com.braintreepayments.api.paypal.PayPalContactPreference;
import com.braintreepayments.api.paypal.PayPalLandingPageType;
import com.braintreepayments.api.paypal.PayPalLineItem;
import com.braintreepayments.api.paypal.PayPalLineItemKind;
import com.braintreepayments.api.paypal.PayPalLineItemUpcType;
import com.braintreepayments.api.paypal.PayPalPaymentIntent;
import com.braintreepayments.api.paypal.PayPalPaymentUserAction;
import com.braintreepayments.api.paypal.PayPalPricingModel;
import com.braintreepayments.api.paypal.PayPalRecurringBillingDetails;
import com.braintreepayments.api.paypal.PayPalRecurringBillingPlanType;
import com.braintreepayments.api.paypal.PayPalPhoneNumber;
import com.braintreepayments.api.paypal.PayPalVaultRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PayPalRequestFactory {

    public static PayPalVaultRequest createPayPalVaultRequest(
        Context context,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber,
        String shopperInsightsSessionId
    ) {

        PayPalVaultRequest request = new PayPalVaultRequest(true);

        if (buyerEmailAddress != null && !buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        if ((buyerPhoneCountryCode != null && !buyerPhoneCountryCode.isEmpty())
            && (buyerPhoneNationalNumber != null && !buyerPhoneNationalNumber.isEmpty())) {

            request.setUserPhoneNumber(new PayPalPhoneNumber(
                buyerPhoneCountryCode,
                buyerPhoneNationalNumber)
            );
        }

        if (shopperInsightsSessionId != null && !shopperInsightsSessionId.isEmpty()) {
            request.setShopperSessionId(shopperInsightsSessionId);
        }

        if (Settings.isPayPalAppSwithEnabled(context)) {
            request.setEnablePayPalAppSwitch(true);
        }

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
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
            postalAddress.setPostalCode("12345");
            postalAddress.setRegion("CA");
            postalAddress.setCountryCodeAlpha2("US");

            request.setShippingAddressOverride(postalAddress);
        }

        if (Settings.isRbaMetadataEnabled(context)) {
            PayPalBillingPricing billingPricing = new PayPalBillingPricing(
                PayPalPricingModel.FIXED,
                "9.99",
                "99.99"
            );

            PayPalBillingCycle billingCycle = new PayPalBillingCycle(
                false,
                1,
                PayPalBillingInterval.MONTH,
                1,
                1,
                "2024-08-01",
                billingPricing
            );

            List<PayPalBillingCycle> billingCycles = new ArrayList<>();
            billingCycles.add(billingCycle);
            PayPalRecurringBillingDetails payPalRecurringBillingDetails = new PayPalRecurringBillingDetails(
                billingCycles,
                "32.56",
                "USD",
                "Vogue Magazine Subscription",
                "9.99",
                "Home delivery to Chicago, IL",
                "19.99",
                1,
                "1.99",
                "0.59"
            );

            request.setRecurringBillingDetails(payPalRecurringBillingDetails);
            request.setRecurringBillingPlanType(PayPalRecurringBillingPlanType.SUBSCRIPTION);
        }

        return request;
    }

    public static PayPalCheckoutRequest createPayPalCheckoutRequest(
        Context context,
        String amount,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber,
        Boolean isContactInformationEnabled,
        String shopperInsightsSessionId,
        Boolean isAmountBreakdownEnabled
    ) {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount, true);

        if (buyerEmailAddress != null && !buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        if ((buyerPhoneCountryCode != null && !buyerPhoneCountryCode.isEmpty())
            && (buyerPhoneNationalNumber != null && !buyerPhoneNationalNumber.isEmpty())) {
            request.setUserPhoneNumber(new PayPalPhoneNumber(
                buyerPhoneCountryCode,
                buyerPhoneNationalNumber)
            );
        }

        if (shopperInsightsSessionId != null && !shopperInsightsSessionId.isEmpty()) {
            request.setShopperSessionId(shopperInsightsSessionId);
        }

        if (Settings.isPayPalAppSwithEnabled(context)) {
            request.setEnablePayPalAppSwitch(true);
        }

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalLandingPageType.LANDING_PAGE_TYPE_LOGIN);
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
            request.setUserAction(PayPalPaymentUserAction.USER_ACTION_COMMIT);
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

        if (isContactInformationEnabled) {
            request.setContactInformation(new PayPalContactInformation("some@email.com", new PayPalPhoneNumber("1", "1234567890")));
            request.setContactPreference(PayPalContactPreference.UPDATE_CONTACT_INFORMATION);
        }

        return request;
    }

    private static List<PayPalLineItem> buildLineItems(Float unitItemPrice,
                                                       Float setupFee,
                                                       Float immediateBillingAmount) {
        Float totalAmount = unitItemPrice
                + (setupFee != null ? setupFee : 0.00f)
                + (immediateBillingAmount != null ? immediateBillingAmount : 0.00f);

        PayPalLineItem item = new PayPalLineItem(
                PayPalLineItemKind.CREDIT,
                "Subscription Setup + First Cycle 54321",
                "1",
                totalAmount.toString()
        );

        item.setDescription("Includes setup and first cycle 12345");
        item.setImageUrl("http://example.com/image.jpg");
        item.setProductCode("sub-setup-001");
        item.setUpcType(PayPalLineItemUpcType.UPC_TYPE_2);
        item.setUpcCode("upc-001");
        item.setUrl("http://example.com");

        // Only include if you use taxTotal in AmountBreakdown
        item.setUnitTaxAmount("0.50");

        return Collections.singletonList(item);
    }
}
