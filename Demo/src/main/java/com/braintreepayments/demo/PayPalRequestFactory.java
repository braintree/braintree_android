package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalRequest;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PostalAddress;

public class PayPalRequestFactory {

    public static PayPalVaultRequest createPayPalVaultRequest(Context context) {

        PayPalVaultRequest request = new PayPalVaultRequest();

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

        return request;
    }

    public static PayPalCheckoutRequest createPayPalCheckoutRequest(Context context, String amount) {
        PayPalCheckoutRequest request = new PayPalCheckoutRequest(amount);

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

            request.setShippingAddressOverride(shippingAddress);
        }

        return request;
    }
}
