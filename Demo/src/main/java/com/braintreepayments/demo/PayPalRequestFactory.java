package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.paypal.PayPalCheckoutRequest;
import com.braintreepayments.api.paypal.PayPalLandingPageType;
import com.braintreepayments.api.paypal.PayPalPaymentIntent;
import com.braintreepayments.api.paypal.PayPalPaymentUserAction;
import com.braintreepayments.api.paypal.PayPalPhoneNumber;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalVaultRequest;

public class PayPalRequestFactory {

    public static PayPalVaultRequest createPayPalVaultRequest(
        Context context,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber
    ) {

        PayPalVaultRequest request = new PayPalVaultRequest(true);

        if (!buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        if (!buyerPhoneCountryCode.isEmpty() && !buyerPhoneNationalNumber.isEmpty()) {
            request.setUserPhoneNumber(new PayPalPhoneNumber(buyerPhoneCountryCode, buyerPhoneNationalNumber));
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
            postalAddress.setRegion("CA");
            postalAddress.setCountryCodeAlpha2("US");

            request.setShippingAddressOverride(postalAddress);
        }

        return request;
    }

    public static PayPalCheckoutRequest createPayPalCheckoutRequest(
        Context context,
        String amount,
        String buyerEmailAddress,
        String buyerPhoneCountryCode,
        String buyerPhoneNationalNumber
    ) {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount, true);

        if (!buyerEmailAddress.isEmpty()) {
            request.setUserAuthenticationEmail(buyerEmailAddress);
        }

        if (!buyerPhoneCountryCode.isEmpty() && !buyerPhoneNationalNumber.isEmpty()) {
            request.setUserPhoneNumber(new PayPalPhoneNumber(buyerPhoneCountryCode, buyerPhoneNationalNumber));
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

        return request;
    }
}
