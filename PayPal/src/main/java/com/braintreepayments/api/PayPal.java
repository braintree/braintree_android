package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.PayPalBrowserSwitchException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;
import org.json.JSONObject;

// TODO: Rename class when API is finalized
public class PayPal {

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;
    private final String returnUrlScheme;

    private final PayPalInternalClient internalPayPalClient;

    public PayPal(BraintreeClient braintreeClient, String returnUrlScheme) {
        this(braintreeClient, returnUrlScheme, new TokenizationClient(braintreeClient), new PayPalInternalClient(braintreeClient, returnUrlScheme));
    }

    // TODO: Make package-private
    public PayPal(BraintreeClient braintreeClient, String returnUrlScheme, TokenizationClient tokenizationClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.returnUrlScheme = returnUrlScheme;
        this.tokenizationClient = tokenizationClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    boolean manifestInvalid(Context context) {
        return !braintreeClient.isUrlSchemeDeclaredInAndroidManifest(context,
                returnUrlScheme, BraintreeBrowserSwitchActivity.class);
    }

    private static Exception createPayPalConfigInvalidException() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    private static Exception createManifestInvalidError() {
        return new BraintreeException("BraintreeBrowserSwitchActivity missing, " +
                "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                "switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration");
    }

    public void requestOneTimePayment(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalRequestCallback callback) {
        if (payPalRequest.getAmount() != null) {
            braintreeClient.sendAnalyticsEvent(activity, "paypal.single-payment.selected");
            if (payPalRequest.shouldOfferCredit()) {
                braintreeClient.sendAnalyticsEvent(activity, "paypal.single-payment.credit.offered");
            }
            
            if (payPalRequest.shouldOfferPayLater()) {
                braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
            }

            braintreeClient.getConfiguration(activity, new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                    if (payPalConfigInvalid(configuration)) {
                        Exception configInvalidError = createPayPalConfigInvalidException();
                        callback.onResult(false, configInvalidError);
                        return;
                    }

                    if (manifestInvalid(activity)) {
                        braintreeClient.sendAnalyticsEvent(activity, "paypal.invalid-manifest");
                        Exception manifestInvalidError = createManifestInvalidError();
                        callback.onResult(false, manifestInvalidError);
                        return;
                    }
                    sendCheckoutRequest(activity, payPalRequest, false, callback);
                }
            });

        } else {
            callback.onResult(false, new BraintreeException("An amount must be specified for the Single Payment flow."));
        }
    }

    public void requestBillingAgreement(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalRequestCallback callback) {
        if (payPalRequest.getAmount() == null) {
            braintreeClient.sendAnalyticsEvent(activity, "paypal.billing-agreement.selected");
            if (payPalRequest.shouldOfferCredit()) {
                braintreeClient.sendAnalyticsEvent(activity, "paypal.billing-agreement.credit.offered");
            }

            braintreeClient.getConfiguration(activity, new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                    if (payPalConfigInvalid(configuration)) {
                        Exception configInvalidError = createPayPalConfigInvalidException();
                        callback.onResult(false, configInvalidError);
                        return;
                    }

                    if (manifestInvalid(activity)) {
                        braintreeClient.sendAnalyticsEvent(activity, "paypal.invalid-manifest");
                        Exception manifestInvalidError = createManifestInvalidError();
                        callback.onResult(false, manifestInvalidError);
                        return;
                    }

                    sendCheckoutRequest(activity, payPalRequest, true, callback);
                }
            });
        } else {
            callback.onResult(false, new BraintreeException("There must be no amount specified for the Billing Agreement flow"));
        }
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalRequest payPalRequest, final boolean isBillingAgreement, final PayPalRequestCallback callback) {
        internalPayPalClient.sendRequest(activity, payPalRequest, isBillingAgreement, new PayPalInternalClientCallback() {
            @Override
            public void onResult(PayPalResponse payPalResponse, Exception error) {
                if (payPalResponse != null) {
                    String analyticsPrefix = getAnalyticsEventPrefix(isBillingAgreement);
                    braintreeClient.sendAnalyticsEvent(activity, String.format("%s.browser-switch.started", analyticsPrefix));

                    try {
                        startBrowserSwitch(activity, payPalResponse);
                        callback.onResult(true, null);
                    } catch (JSONException | BrowserSwitchException exception) {
                        callback.onResult(false, exception);
                    }
                } else {
                    callback.onResult(false, error);
                }
            }
        });
    }

    private void startBrowserSwitch(FragmentActivity activity, PayPalResponse payPalResponse) throws JSONException, BrowserSwitchException {
        JSONObject metadata = new JSONObject();
        metadata.put("approval-url", payPalResponse.getApprovalUrl());
        metadata.put("success-url", payPalResponse.getSuccessUrl());

        String paymentType = payPalResponse.isBillingAgreement()
                ? "billing-agreement" : "single-payment";

        metadata.put("payment-type", paymentType);
        metadata.put("client-metadata-id", payPalResponse.getClientMetadataId());
        metadata.put("merchant-account-id", payPalResponse.getMerchantAccountId());
        metadata.put("source", "paypal-browser");
        metadata.put("intent", payPalResponse.getIntent());

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.PAYPAL)
                .url(Uri.parse(payPalResponse.getApprovalUrl()))
                .metadata(metadata);
        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }

    private static String getAnalyticsEventPrefix(boolean isBillingAgreement) {
        return isBillingAgreement ? "paypal.billing-agreement" : "paypal.single-payment";
    }

    public void onBrowserSwitchResult(final Context context, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri, final PayPalBrowserSwitchResultCallback callback) {

        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String clientMetadataId = Json.optString(metadata, "client-metadata-id", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);
        String payPalIntent = Json.optString(metadata, "intent", null);
        String approvalUrl = Json.optString(metadata, "approval-url", null);
        String successUrl = Json.optString(metadata, "success-url", null);
        String paymentType = Json.optString(metadata, "payment-type", "unknown");

        boolean isBillingAgreement = paymentType.equalsIgnoreCase("billing-agreement");
        String tokenKey = isBillingAgreement ? "ba_token" : "token";
        String analyticsPrefix = getAnalyticsEventPrefix(isBillingAgreement);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchResult.STATUS_CANCELED:
                callback.onResult(null, new BraintreeException("User Canceled PayPal"));
                braintreeClient.sendAnalyticsEvent(context, String.format("%s.browser-switch.canceled", analyticsPrefix));
                break;
            case BrowserSwitchResult.STATUS_OK:
                try {
                    JSONObject oneTouchCoreData = createOneTouchPayload(uri, successUrl, approvalUrl, tokenKey);
                    PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                            .clientMetadataId(clientMetadataId)
                            .intent(payPalIntent)
                            .source("paypal-browser")
                            .oneTouchCoreData(oneTouchCoreData);

                    if (merchantAccountId != null) {
                        payPalAccountBuilder.merchantAccountId(merchantAccountId);
                    }

                    if (payPalIntent != null) {
                        payPalAccountBuilder.intent(payPalIntent);
                    }

                    tokenizationClient.tokenize(context, payPalAccountBuilder, new PaymentMethodNonceCallback() {
                        @Override
                        public void success(PaymentMethodNonce paymentMethodNonce) {
                            if (paymentMethodNonce instanceof PayPalAccountNonce &&
                                    ((PayPalAccountNonce) paymentMethodNonce).getCreditFinancing() != null) {
                                braintreeClient.sendAnalyticsEvent(context, "paypal.credit.accepted");
                            }

                            callback.onResult(paymentMethodNonce, null);
                        }

                        @Override
                        public void failure(Exception exception) {
                            callback.onResult(null, exception);
                        }
                    });

                    braintreeClient.sendAnalyticsEvent(context, String.format("%s.browser-switch.succeeded", analyticsPrefix));

                } catch (JSONException | PayPalBrowserSwitchException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(context, String.format("%s.browser-switch.failed", analyticsPrefix));
                }
                break;
        }
    }

    public JSONObject createOneTouchPayload(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new PayPalBrowserSwitchException("User cancelled.");
        }

        String requestXoToken = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
        String responseXoToken = uri.getQueryParameter(tokenKey);
        if (responseXoToken != null && TextUtils.equals(requestXoToken, responseXoToken)) {
            JSONObject client = new JSONObject();
            client.put("environment", null);

            JSONObject oneTouchData = new JSONObject();
            oneTouchData.put("client", client);

            JSONObject response = new JSONObject();
            response.put("webURL", uri.toString());
            oneTouchData.put("response", response);

            oneTouchData.put("response_type", "web");

            return oneTouchData;
        } else {
            throw new PayPalBrowserSwitchException("The response contained inconsistent data.");
        }
    }
}
