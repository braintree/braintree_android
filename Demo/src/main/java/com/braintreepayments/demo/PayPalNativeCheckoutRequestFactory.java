package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.PayPalNativeCheckoutRequest;
import com.braintreepayments.api.PayPalNativeCheckoutPaymentIntent;
import com.braintreepayments.api.PayPalNativeRequest;
import com.braintreepayments.api.PayPalNativeCheckoutVaultRequest;
import com.braintreepayments.api.PostalAddress;
import com.braintreepayments.api.PayPalNativeCheckoutConfig;

import java.util.UUID;

public class PayPalNativeCheckoutRequestFactory {

    public static PayPalNativeCheckoutVaultRequest createPayPalVaultRequest(Context context) {
        PayPalNativeCheckoutVaultRequest request = new PayPalNativeCheckoutVaultRequest();

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalNativeRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalNativeRequest.LANDING_PAGE_TYPE_LOGIN);
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

        PayPalNativeCheckoutConfig nativeConfig = new PayPalNativeCheckoutConfig();
        nativeConfig.setCorrelationId(UUID.randomUUID().toString());
        nativeConfig.setReturnUrl("com.braintreepayments.demo://paypalpay");

        request.setNativeConfig(nativeConfig);
        return request;
    }

    public static PayPalNativeCheckoutRequest createPayPalCheckoutRequest(Context context, String amount) {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest(amount);

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        String landingPageType = Settings.getPayPalLandingPageType(context);
        if (context.getString(R.string.paypal_landing_page_type_billing).equals(landingPageType)) {
            request.setLandingPageType(PayPalNativeRequest.LANDING_PAGE_TYPE_BILLING);
        } else if (context.getString(R.string.paypal_landing_page_type_login).equals(landingPageType)) {
            request.setLandingPageType(PayPalNativeRequest.LANDING_PAGE_TYPE_LOGIN);
        }

        String intentType = Settings.getPayPalIntentType(context);
        if (intentType.equals(context.getString(R.string.paypal_intent_authorize))) {
            request.setIntent(PayPalNativeCheckoutPaymentIntent.AUTHORIZE);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_order))) {
            request.setIntent(PayPalNativeCheckoutPaymentIntent.ORDER);
        } else if (intentType.equals(context.getString(R.string.paypal_intent_sale))) {
            request.setIntent(PayPalNativeCheckoutPaymentIntent.SALE);
        }

        if (Settings.isPayPalUseractionCommitEnabled(context)) {
            request.setUserAction(PayPalNativeCheckoutRequest.USER_ACTION_COMMIT);
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

        PayPalNativeCheckoutConfig nativeConfig = new PayPalNativeCheckoutConfig();
        nativeConfig.setCorrelationId(UUID.randomUUID().toString());
        nativeConfig.setReturnUrl("com.braintreepayments.demo://paypalpay");

        request.setNativeConfig(nativeConfig);
        return request;
    }
}
