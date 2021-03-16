package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.PayPalCheckoutRequest;
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

        if (Settings.isPayPalUseractionCommitEnabled(context)) {
            request.setUserAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.isPayPalCreditOffered(context)) {
            request.setOfferCredit(true);
        }

        if (Settings.usePayPalAddressOverride(context)) {
            request.setShippingAddressOverride(new PostalAddress()
                    .recipientName("Brian Tree")
                    .streetAddress("123 Fake Street")
                    .extendedAddress("Floor A")
                    .locality("San Francisco")
                    .region("CA")
                    .countryCodeAlpha2("US")
            );
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
            request.setIntent(PayPalCheckoutRequest.INTENT_AUTHORIZE);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_order))) {
            request.setIntent(PayPalCheckoutRequest.INTENT_ORDER);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_sale))) {
            request.setIntent(PayPalCheckoutRequest.INTENT_SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(context)) {
            request.setUserAction(PayPalRequest.USER_ACTION_COMMIT);
        }

        if (Settings.usePayPalAddressOverride(context)) {
            request.setShippingAddressOverride(new PostalAddress()
                    .recipientName("Brian Tree")
                    .streetAddress("123 Fake Street")
                    .extendedAddress("Floor A")
                    .locality("San Francisco")
                    .region("CA")
                    .countryCodeAlpha2("US")
            );
        }

        return request;
    }
}
