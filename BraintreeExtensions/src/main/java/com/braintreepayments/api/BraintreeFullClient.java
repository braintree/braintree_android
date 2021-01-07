package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsCallback;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.ReadyForGooglePaymentRequest;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.api.models.UnionPayCardBuilder;
import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;

// TODO: unit test when API is finalized
class BraintreeFullClient {

    AmericanExpress americanExpress;
    BraintreeClient braintreeClient;
    DataCollector dataCollector;
    Card card;
    DeviceInspector deviceInspector;
    GooglePaymentClient googlePaymentClient;
    LocalPayment localPayment;
    PayPal payPal;
    PreferredPaymentMethods preferredPaymentMethods;
    ThreeDSecure threeDSecure;
    TokenizationClient tokenizationClient;
    UnionPay unionPay;
    Venmo venmo;
    VisaCheckoutClient visaCheckoutClient;

    public BraintreeFullClient(String authorization, Context context, String returnUrlScheme) throws InvalidArgumentException {
        this.braintreeClient = new BraintreeClient(Authorization.fromString(authorization), context, returnUrlScheme);
        this.dataCollector = new DataCollector(braintreeClient);
        this.tokenizationClient = new TokenizationClient(braintreeClient);

        this.americanExpress = new AmericanExpress(braintreeClient);
        this.card = new Card(braintreeClient, tokenizationClient, dataCollector);
        this.deviceInspector = new DeviceInspector();
        this.googlePaymentClient = new GooglePaymentClient(braintreeClient);
        this.localPayment = new LocalPayment(returnUrlScheme, braintreeClient);
        this.payPal = new PayPal(braintreeClient, returnUrlScheme);
        this.preferredPaymentMethods = new PreferredPaymentMethods(braintreeClient);
        this.threeDSecure = new ThreeDSecure(braintreeClient, returnUrlScheme, tokenizationClient);
        this.unionPay = new UnionPay(braintreeClient, tokenizationClient);
        this.venmo = new Venmo(braintreeClient, tokenizationClient);
        this.visaCheckoutClient = new VisaCheckoutClient(braintreeClient, tokenizationClient);
    }

    public void getConfiguration(Context context, ConfigurationCallback callback) {
        braintreeClient.getConfiguration(callback);
    }

    public void collectDeviceData(Context context, BraintreeDataCollectorCallback callback) {
        dataCollector.collectDeviceData(context, callback);
    }

    public void fetchUnionPayCapabilities(Context context, String cardNumber, UnionPayFetchCapabilitiesCallback callback) {
        unionPay.fetchCapabilities(context, cardNumber, callback);
    }

    public void enrollUnionPay(Context context, UnionPayCardBuilder unionPayCardBuilder, UnionPayEnrollCallback callback) {
        unionPay.enroll(context, unionPayCardBuilder, callback);
    }

    public void tokenizeUnionPay(Context context, UnionPayCardBuilder unionPayCardBuilder, UnionPayTokenizeCallback callback) {
        unionPay.tokenize(context, unionPayCardBuilder, callback);
    }

    public void tokenizeCard(Context context, CardBuilder cardBuilder, CardTokenizeCallback callback) {
        card.tokenize(context, cardBuilder, callback);
    }

    public void performThreeDSecureVerification(FragmentActivity activity, ThreeDSecureRequest request, ThreeDSecureLookupCallback callback) {
        threeDSecure.performVerification(activity, request, callback);
    }

    public void continuePerformVerification(FragmentActivity activity, ThreeDSecureRequest request, ThreeDSecureLookup threeDSecureLookup, ThreeDSecureVerificationCallback callback) {
        threeDSecure.continuePerformVerification(activity, request, threeDSecureLookup, callback);
    }

    public void getAmericanExpressRewards(Context context, String nonce, String currencyIsoCode, AmericanExpressGetRewardsBalanceCallback callback) {
        americanExpress.getRewardsBalance(context, nonce, currencyIsoCode, callback);
    }

    public void requestPayPalOneTimePayment(FragmentActivity activity, PayPalRequest request, PayPalRequestCallback callback) {
        payPal.requestOneTimePayment(activity, request, callback);
    }

    public void requestPayPalBillingAgreement(FragmentActivity activity, PayPalRequest request, PayPalRequestCallback callback) {
        payPal.requestBillingAgreement(activity, request, callback);
    }

    public void fetchPreferredPaymentMethods(Context context, PreferredPaymentMethodsCallback callback) {
        preferredPaymentMethods.fetchPreferredPaymentMethods(context, callback);
    }

    public void authorizeVenmoAccount(FragmentActivity activity, boolean vault, String profileId, VenmoAuthorizeAccountCallback callback) {
        venmo.authorizeAccount(activity, vault, profileId, callback);
    }

    public boolean isVenmoAppSwitchEnabled(Context context) {
        return deviceInspector.isVenmoAppSwitchAvailable(context);
    }

    public void startLocalPayment(Context context, LocalPaymentRequest localPaymentRequest, LocalPaymentStartCallback callback) {
        localPayment.startPayment(context, localPaymentRequest, callback);
    }

    public void approveLocalPayment(FragmentActivity activity, LocalPaymentTransaction transaction) throws JSONException, BrowserSwitchException {
        localPayment.approveTransaction(activity, transaction);
    }

    public void onPayPalBrowserSwitchResult(Context context, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri, PayPalBrowserSwitchResultCallback callback) {
        payPal.onBrowserSwitchResult(context, browserSwitchResult, uri, callback);
    }

    public void onThreeDSecureBrowserSwitchResult(Context context, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri, ThreeDSecureResultCallback callback) {
        threeDSecure.onBrowserSwitchResult(context, browserSwitchResult, uri, callback);
    }

    public void onThreeDSecureActivityResult(Context context, int resultCode, Intent data, ThreeDSecureResultCallback callback) {
        threeDSecure.onActivityResult(context, resultCode, data, callback);
    }

    public void onVenmoActivityResult(Context context, int resultCode, Intent data, VenmoOnActivityResultCallback callback) {
        venmo.onActivityResult(context, resultCode, data, callback);
    }

    public void onVisaCheckoutActivityResult(Context context, int resultCode, Intent data, VisaCheckoutOnActivityResultCallback callback) {
        visaCheckoutClient.onActivityResult(context, resultCode, data, callback);
    }

    public void onLocalPaymentBrowserSwitchResult(Context context, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri, LocalPaymentBrowserSwitchResultCallback callback) {
        localPayment.onBrowserSwitchResult(context, browserSwitchResult, uri, callback);
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
