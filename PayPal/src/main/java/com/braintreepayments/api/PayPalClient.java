package com.braintreepayments.api;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize PayPal accounts. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/">documentation</a>
 */
public class PayPalClient {

    private final BraintreeClient braintreeClient;
    private final TokenizationClient tokenizationClient;

    private final PayPalInternalClient internalPayPalClient;

    public PayPalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new TokenizationClient(braintreeClient), new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(BraintreeClient braintreeClient, TokenizationClient tokenizationClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.tokenizationClient = tokenizationClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private boolean browserSwitchNotPossible(FragmentActivity activity) {
        return !braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.PAYPAL);
    }

    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    private static Exception createBrowserSwitchError() {
        return new BraintreeException("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration");
    }

    /**
     * Tokenize a PayPal account for vault or checkout.
     *
     * @param activity Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     * @param callback {@link PayPalFlowStartedCallback}
     */
    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalFlowStartedCallback callback) {
       if (payPalRequest instanceof PayPalCheckoutRequest) {
           sendCheckoutRequest(activity, (PayPalCheckoutRequest) payPalRequest, callback);
       } else if (payPalRequest instanceof PayPalVaultRequest) {
           sendVaultRequest(activity, (PayPalVaultRequest) payPalRequest, callback);
       }
    }

    /**
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)} instead.
     * Starts the One-Time Payment (Checkout) flow for PayPal.
     *
     * @param activity Android FragmentActivity
     * @param payPalCheckoutRequest a {@link PayPalCheckoutRequest} used to customize the request.
     * @param callback {@link PayPalFlowStartedCallback}
     */
    @Deprecated
    public void requestOneTimePayment(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest, final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalCheckoutRequest, callback);
    }

    /**
     * @deprecated Use {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)} instead.
     * Starts the Billing Agreement (Vault) flow for PayPal.
     *
     * @param activity Android FragmentActivity
     * @param payPalVaultRequest a {@link PayPalVaultRequest} used to customize the request.
     * @param callback {@link PayPalFlowStartedCallback}
     */
    @Deprecated
    public void requestBillingAgreement(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest, final PayPalFlowStartedCallback callback) {
        tokenizePayPalAccount(activity, payPalVaultRequest, callback);
    }

    private void sendCheckoutRequest(final FragmentActivity activity, final PayPalCheckoutRequest payPalCheckoutRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected");
        if (payPalCheckoutRequest.shouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    callback.onResult(manifestInvalidError);
                    return;
                }
                sendPayPalRequest(activity, payPalCheckoutRequest, callback);
            }
        });

    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalVaultRequest payPalVaultRequest, final PayPalFlowStartedCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected");
        if (payPalVaultRequest.shouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable final Configuration configuration, @Nullable Exception error) {
                if (payPalConfigInvalid(configuration)) {
                    Exception configInvalidError = createPayPalError();
                    callback.onResult(configInvalidError);
                    return;
                }

                if (browserSwitchNotPossible(activity)) {
                    braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                    Exception manifestInvalidError = createBrowserSwitchError();
                    callback.onResult(manifestInvalidError);
                    return;
                }

                sendPayPalRequest(activity, payPalVaultRequest, callback);
            }
        });
    }

    private void sendPayPalRequest(final FragmentActivity activity, final PayPalRequest payPalRequest, final PayPalFlowStartedCallback callback) {
        internalPayPalClient.sendRequest(activity, payPalRequest, new PayPalInternalClientCallback() {
            @Override
            public void onResult(PayPalResponse payPalResponse, Exception error) {
                if (payPalResponse != null) {
                    String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.started", analyticsPrefix));

                    try {
                        startBrowserSwitch(activity, payPalResponse);
                        callback.onResult(null);
                    } catch (JSONException | BrowserSwitchException exception) {
                        callback.onResult(exception);
                    }
                } else {
                    callback.onResult(error);
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
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .metadata(metadata);
        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
    }

    private static String getAnalyticsEventPrefix(PayPalRequest request) {
        return request instanceof PayPalVaultRequest ? "paypal.billing-agreement" : "paypal.single-payment";
    }

    /**
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback {@link PayPalBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(BrowserSwitchResult browserSwitchResult, final PayPalBrowserSwitchResultCallback callback) {
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String clientMetadataId = Json.optString(metadata, "client-metadata-id", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);
        String payPalIntent = Json.optString(metadata, "intent", null);
        String approvalUrl = Json.optString(metadata, "approval-url", null);
        String successUrl = Json.optString(metadata, "success-url", null);
        String paymentType = Json.optString(metadata, "payment-type", "unknown");

        boolean isBillingAgreement = paymentType.equalsIgnoreCase("billing-agreement");
        String tokenKey = isBillingAgreement ? "ba_token" : "token";
        String analyticsPrefix = isBillingAgreement ? "paypal.billing-agreement" : "paypal.single-payment";

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                callback.onResult(null, new BraintreeException("User Canceled PayPal"));
                braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.canceled", analyticsPrefix));
                break;
            case BrowserSwitchStatus.SUCCESS:
                try {
                    Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                    if (deepLinkUri != null) {
                        JSONObject urlResponseData = parseUrlResponseData(deepLinkUri, successUrl, approvalUrl, tokenKey);
                        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                                .clientMetadataId(clientMetadataId)
                                .intent(payPalIntent)
                                .source("paypal-browser")
                                .urlResponseData(urlResponseData);

                        if (merchantAccountId != null) {
                            payPalAccountBuilder.merchantAccountId(merchantAccountId);
                        }

                        if (payPalIntent != null) {
                            payPalAccountBuilder.intent(payPalIntent);
                        }

                        tokenizationClient.tokenize(payPalAccountBuilder, new PaymentMethodNonceCallback() {
                            @Override
                            public void success(PaymentMethodNonce paymentMethodNonce) {
                                if (paymentMethodNonce instanceof PayPalAccountNonce) {
                                    PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) paymentMethodNonce;

                                    if (payPalAccountNonce.getCreditFinancing() != null) {
                                        braintreeClient.sendAnalyticsEvent("paypal.credit.accepted");
                                    }
                                    callback.onResult(payPalAccountNonce, null);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                callback.onResult(null, exception);
                            }
                        });

                        braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.succeeded", analyticsPrefix));
                    } else {
                        callback.onResult(null, new BraintreeException("Unknown error"));
                    }
                } catch (JSONException | PayPalBrowserSwitchException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(String.format("%s.browser-switch.failed", analyticsPrefix));
                }
                break;
        }
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl, String tokenKey) throws JSONException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new PayPalBrowserSwitchException("User cancelled.");
        }

        String requestXoToken = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
        String responseXoToken = uri.getQueryParameter(tokenKey);
        if (responseXoToken != null && TextUtils.equals(requestXoToken, responseXoToken)) {
            JSONObject client = new JSONObject();
            client.put("environment", null);

            JSONObject urlResponseData = new JSONObject();
            urlResponseData.put("client", client);

            JSONObject response = new JSONObject();
            response.put("webURL", uri.toString());
            urlResponseData.put("response", response);

            urlResponseData.put("response_type", "web");

            return urlResponseData;
        } else {
            throw new PayPalBrowserSwitchException("The response contained inconsistent data.");
        }
    }
}
