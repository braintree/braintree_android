package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to tokenize PayPal accounts. For more information see the <a
 * href="https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4">documentation</a>
 */
public class PayPalClient {

    private final BraintreeClient braintreeClient;
    private final PayPalInternalClient internalPayPalClient;

    /**
     * Initializes a new {@link PayPalClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public PayPalClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

    @VisibleForTesting
    PayPalClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(BraintreeClient braintreeClient,
                 PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private void assertCanPerformBrowserSwitch(FragmentActivity activity)
            throws BrowserSwitchException {
        braintreeClient.assertCanPerformBrowserSwitch(activity, BraintreeRequestCodes.PAYPAL);
    }

    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4 " +
                "for more information.");
    }

    private static Exception createBrowserSwitchError(BrowserSwitchException exception) {
        return new BraintreeException(
                "AndroidManifest.xml is incorrectly configured or another app " +
                        "defines the same browser switch url as this app. See " +
                        "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                        "for the correct configuration: " + exception.getMessage());
    }

    /**
     * Starts the PayPal payment flow by creating a {@link PayPalPaymentAuthRequestParams} to be used to
     * launch the PayPal web authentication flow in
     * {@link PayPalLauncher#launch(FragmentActivity, PayPalPaymentAuthRequestParams)}.
     *
     * @param activity      Android FragmentActivity
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     * @param callback      {@link PayPalPaymentAuthCallback}
     */
    public void createPaymentAuthRequest(@NonNull final FragmentActivity activity,
                                         @NonNull final PayPalRequest payPalRequest,
                                         @NonNull final PayPalPaymentAuthCallback callback) {
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            sendCheckoutRequest(activity, (PayPalCheckoutRequest) payPalRequest, callback);
        } else if (payPalRequest instanceof PayPalVaultRequest) {
            sendVaultRequest(activity, (PayPalVaultRequest) payPalRequest, callback);
        }
    }

    private void sendCheckoutRequest(final FragmentActivity activity,
                                     final PayPalCheckoutRequest payPalCheckoutRequest,
                                     final PayPalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected");
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (error != null) {
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(error));
                return;
            }

            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(configInvalidError));
                return;
            }

            try {
                assertCanPerformBrowserSwitch(activity);
            } catch (BrowserSwitchException browserSwitchException) {
                braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                Exception manifestInvalidError =
                        createBrowserSwitchError(browserSwitchException);
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(manifestInvalidError));
                return;
            }
            sendPayPalRequest(activity, payPalCheckoutRequest, callback);
        });

    }

    private void sendVaultRequest(final FragmentActivity activity,
                                  final PayPalVaultRequest payPalVaultRequest,
                                  final PayPalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected");
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (error != null) {
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(error));
                return;
            }

            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(configInvalidError));
                return;
            }

            try {
                assertCanPerformBrowserSwitch(activity);
            } catch (BrowserSwitchException browserSwitchException) {
                braintreeClient.sendAnalyticsEvent("paypal.invalid-manifest");
                Exception manifestInvalidError =
                        createBrowserSwitchError(browserSwitchException);
                callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(manifestInvalidError));
                return;
            }
            sendPayPalRequest(activity, payPalVaultRequest, callback);
        });
    }

    private void sendPayPalRequest(final FragmentActivity activity,
                                   final PayPalRequest payPalRequest,
                                   final PayPalPaymentAuthCallback callback) {
        internalPayPalClient.sendRequest(activity, payPalRequest,
                (payPalResponse, error) -> {
                    if (payPalResponse != null) {
                        String analyticsPrefix = getAnalyticsEventPrefix(payPalRequest);
                        braintreeClient.sendAnalyticsEvent(
                                String.format("%s.browser-switch.started", analyticsPrefix));

                        try {
                            BrowserSwitchOptions options =
                                    buildBrowserSwitchOptions(payPalResponse);
                            payPalResponse.setBrowserSwitchOptions(options);
                            callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.ReadyToLaunch(payPalResponse));
                        } catch (JSONException exception) {
                            callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(exception));
                        }
                    } else {
                        callback.onPayPalPaymentAuthRequest(new PayPalPaymentAuthRequest.Failure(error));
                    }
                });
    }

    private BrowserSwitchOptions buildBrowserSwitchOptions(
            PayPalPaymentAuthRequestParams paymentAuthRequest)
            throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put("approval-url", paymentAuthRequest.getApprovalUrl());
        metadata.put("success-url", paymentAuthRequest.getSuccessUrl());

        String paymentType = paymentAuthRequest.isBillingAgreement()
                ? "billing-agreement" : "single-payment";

        metadata.put("payment-type", paymentType);
        metadata.put("client-metadata-id", paymentAuthRequest.getClientMetadataId());
        metadata.put("merchant-account-id", paymentAuthRequest.getMerchantAccountId());
        metadata.put("source", "paypal-browser");
        metadata.put("intent", paymentAuthRequest.getIntent());

        return new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.PAYPAL)
                .url(Uri.parse(paymentAuthRequest.getApprovalUrl()))
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .metadata(metadata);
    }

    private static String getAnalyticsEventPrefix(PayPalRequest request) {
        return request instanceof PayPalVaultRequest ? "paypal.billing-agreement" :
                "paypal.single-payment";
    }

    /**
     * After receiving a result from the PayPal web authentication flow via
     * {@link PayPalLauncher#handleReturnToAppFromBrowser(Context, Intent)}, pass the
     * {@link PayPalPaymentAuthResult} returned to this method to tokenize the PayPal account and
     * receive a {@link PayPalAccountNonce} on success.
     *
     * @param paymentAuthResult a {@link PayPalPaymentAuthResult} received in the callback
     *                                  of {@link PayPalLauncher#PayPalLauncher(PayPalLauncherCallback)}
     * @param callback                  {@link PayPalTokenizeCallback}
     */
    public void tokenize(@NonNull PayPalPaymentAuthResult paymentAuthResult,
                         @NonNull final PayPalTokenizeCallback callback) {
        //noinspection ConstantConditions
        if (paymentAuthResult == null) {
            callback.onPayPalResult(new PayPalResult.Failure(
                    new BraintreeException("PayPalBrowserSwitchResult cannot be null")));
            return;
        }
        BrowserSwitchResult browserSwitchResult =
                paymentAuthResult.getBrowserSwitchResult();
        if (browserSwitchResult == null && paymentAuthResult.getError() != null) {
            callback.onPayPalResult(new PayPalResult.Failure(paymentAuthResult.getError()));
            return;
        }
        if (browserSwitchResult == null) {
            callback.onPayPalResult(new PayPalResult.Failure(new BraintreeException("An unexpected error occurred")));
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
        String analyticsPrefix =
                isBillingAgreement ? "paypal.billing-agreement" : "paypal.single-payment";

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                callback.onPayPalResult(PayPalResult.Cancel.INSTANCE);
                braintreeClient.sendAnalyticsEvent(
                        String.format("%s.browser-switch.canceled", analyticsPrefix));
                break;
            case BrowserSwitchStatus.SUCCESS:
                try {
                    Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                    if (deepLinkUri != null) {
                        JSONObject urlResponseData =
                                parseUrlResponseData(deepLinkUri, successUrl, approvalUrl,
                                        tokenKey);
                        PayPalAccount payPalAccount = new PayPalAccount();
                        payPalAccount.setClientMetadataId(clientMetadataId);
                        payPalAccount.setIntent(payPalIntent);
                        payPalAccount.setSource("paypal-browser");
                        payPalAccount.setUrlResponseData(urlResponseData);
                        payPalAccount.setPaymentType(paymentType);

                        if (merchantAccountId != null) {
                            payPalAccount.setMerchantAccountId(merchantAccountId);
                        }

                        if (payPalIntent != null) {
                            payPalAccount.setIntent(payPalIntent);
                        }

                        internalPayPalClient.tokenize(payPalAccount,
                                (payPalAccountNonce, error) -> {
                                    if (payPalAccountNonce != null &&
                                            payPalAccountNonce.getCreditFinancing() != null) {
                                        braintreeClient.sendAnalyticsEvent(
                                                "paypal.credit.accepted");
                                    }
                                    if (payPalAccountNonce != null) {
                                        callback.onPayPalResult(new PayPalResult.Success(payPalAccountNonce));
                                    } else if (error != null) {
                                        callback.onPayPalResult(new PayPalResult.Failure(error));
                                    }
                                });

                        braintreeClient.sendAnalyticsEvent(
                                String.format("%s.browser-switch.succeeded", analyticsPrefix));
                    } else {
                        callback.onPayPalResult(new PayPalResult.Failure(new BraintreeException("Unknown error")));
                    }
                } catch (UserCanceledException e) {
                    callback.onPayPalResult(PayPalResult.Cancel.INSTANCE);
                    braintreeClient.sendAnalyticsEvent(
                            String.format("%s.browser-switch.canceled", analyticsPrefix));
                } catch (JSONException | PayPalBrowserSwitchException e) {
                    callback.onPayPalResult(new PayPalResult.Failure(e));
                    braintreeClient.sendAnalyticsEvent(
                            String.format("%s.browser-switch.failed", analyticsPrefix));
                }
                break;
        }
    }

    private JSONObject parseUrlResponseData(Uri uri, String successUrl, String approvalUrl,
                                            String tokenKey)
            throws JSONException, UserCanceledException, PayPalBrowserSwitchException {
        String status = uri.getLastPathSegment();

        if (!Uri.parse(successUrl).getLastPathSegment().equals(status)) {
            throw new UserCanceledException("User canceled PayPal.", true);
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
