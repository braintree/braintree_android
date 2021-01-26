package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import com.visa.checkout.VisaPaymentSummary;

// TODO: unit test when API is finalized
class BraintreeFullClient {

    BraintreeClient braintreeClient;
    DeviceInspector deviceInspector;
    GooglePaymentClient googlePaymentClient;
    PreferredPaymentMethods preferredPaymentMethods;
    TokenizationClient tokenizationClient;
    VisaCheckoutClient visaCheckoutClient;

    public BraintreeFullClient(String authorization, Context context, String returnUrlScheme) throws InvalidArgumentException {
        this.braintreeClient = new BraintreeClient(Authorization.fromString(authorization), context, returnUrlScheme);
        this.tokenizationClient = new TokenizationClient(braintreeClient);

        this.deviceInspector = new DeviceInspector();
        this.googlePaymentClient = new GooglePaymentClient(braintreeClient);
        this.preferredPaymentMethods = new PreferredPaymentMethods(braintreeClient);
        this.visaCheckoutClient = new VisaCheckoutClient(braintreeClient, tokenizationClient);
    }

    public void getConfiguration(Context context, ConfigurationCallback callback) {
        braintreeClient.getConfiguration(callback);
    }

    public void fetchPreferredPaymentMethods(Context context, PreferredPaymentMethodsCallback callback) {
        preferredPaymentMethods.fetchPreferredPaymentMethods(context, callback);
    }

    public void onVisaCheckoutActivityResult(Context context, int resultCode, Intent data, VisaCheckoutOnActivityResultCallback callback) {
        visaCheckoutClient.onActivityResult(context, resultCode, data, callback);
    }

    public void onGooglePayActivityResult(FragmentActivity activity, int resultCode, Intent data, GooglePaymentOnActivityResultCallback callback) {
        googlePaymentClient.onActivityResult(activity, resultCode, data, callback);
    }

    public void deliverBrowserSwitchResult(FragmentActivity activity) {
        braintreeClient.deliverBrowserSwitchResult(activity);
    }

    public void createVisaCheckoutProfile(Context context, VisaCheckoutCreateProfileBuilderCallback callback) {
        visaCheckoutClient.createProfileBuilder(context, callback);
    }

    public void tokenizeVisaCheckout(Context context, VisaPaymentSummary visaPaymentSummary, VisaCheckoutTokenizeCallback callback) {
        visaCheckoutClient.tokenize(context, visaPaymentSummary, callback);
    }

    public void googlePayIsReadyToPay(FragmentActivity activity, ReadyForGooglePaymentRequest request, GooglePaymentIsReadyToPayCallback callback) {
        googlePaymentClient.isReadyToPay(activity, request, callback);
    }

    public void googleRequestPayment(FragmentActivity activity, GooglePaymentRequest request, GooglePaymentRequestPaymentCallback callback) {
        googlePaymentClient.requestPayment(activity, request, callback);
    }
}
