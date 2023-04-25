package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.PayPalNativeCheckoutPaymentIntent;
import com.braintreepayments.api.PayPalNativeCheckoutRequest;
import com.braintreepayments.api.PayPalNativeCheckoutVaultRequest;
import com.braintreepayments.api.PostalAddress;

public class PayPalNativeCheckoutRequestFactory {

    public static PayPalNativeCheckoutVaultRequest createPayPalVaultRequest(Context context) {
        PayPalNativeCheckoutVaultRequest request = new PayPalNativeCheckoutVaultRequest();

        request.setDisplayName(Settings.getPayPalDisplayName(context));

        if (Settings.isPayPalCreditOffered(context)) {
            request.setShouldOfferCredit(true);
        }

        request.setReturnUrl("com.braintreepayments.demo://paypalpay");
        return request;
    }

    public static PayPalNativeCheckoutRequest createPayPalCheckoutRequest(Context context, String amount) {
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest(amount);

        request.setDisplayName(Settings.getPayPalDisplayName(context));

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
            request.setShippingAddressEditable(true);
            request.setShippingAddressRequired(true);
            PostalAddress shippingAddress = new PostalAddress();
            shippingAddress.setRecipientName("Brian Tree");
            shippingAddress.setStreetAddress("123 Fake Street");
            shippingAddress.setExtendedAddress("Floor A");
            shippingAddress.setPostalCode("94103");
            shippingAddress.setLocality("San Francisco");
            shippingAddress.setRegion("CA");
            shippingAddress.setCountryCodeAlpha2("US");

            request.setShippingAddressOverride(shippingAddress);
        }
        request.setReturnUrl("com.braintreepayments.demo://paypalpay");
        return request;
    }
}
