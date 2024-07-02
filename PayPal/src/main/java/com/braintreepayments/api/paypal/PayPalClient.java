package com.braintreepayments.api.paypal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.BrowserSwitchFinalResult;
import com.braintreepayments.api.BrowserSwitchOptions;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.UserCanceledException;
import com.braintreepayments.api.sharedutils.Json;

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
     * Used for linking events from the client to server side request
     * In the PayPal flow this will be either an EC token or a Billing Agreement token
     */
    private String payPalContextId = null;

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private Boolean isVaultRequest = false;

    /**
     * Initializes a new {@link PayPalClient} instance
     *
     * @param context          an Android Context
     * @param authorization    a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl A {@link Uri} containing the Android App Link website associated with
     *                         your application to be used to return to your app from the PayPal
     *                         payment flows.
     */
    public PayPalClient(@NonNull Context context,
                        @NonNull String authorization,
                        @NonNull Uri appLinkReturnUrl) {
        this(new BraintreeClient(context, authorization, null, appLinkReturnUrl));
    }

    @VisibleForTesting
    PayPalClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalInternalClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalClient(BraintreeClient braintreeClient, PayPalInternalClient internalPayPalClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = internalPayPalClient;
    }

    private static boolean payPalConfigInvalid(Configuration configuration) {
        return (configuration == null || !configuration.isPayPalEnabled());
    }

    private static Exception createPayPalError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4 " +
                "for more information.");
    }

    /**
     * Starts the PayPal payment flow by creating a {@link PayPalPaymentAuthRequestParams} to be
     * used to launch the PayPal web authentication flow in
     * {@link PayPalLauncher#launch(ComponentActivity, PayPalPaymentAuthRequest.ReadyToLaunch)}.
     *
     * @param context       Android Context
     * @param payPalRequest a {@link PayPalRequest} used to customize the request.
     * @param callback      {@link PayPalPaymentAuthCallback}
     */
    public void createPaymentAuthRequest(@NonNull final Context context,
                                         @NonNull final PayPalRequest payPalRequest,
                                         @NonNull final PayPalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_STARTED);
        if (payPalRequest instanceof PayPalCheckoutRequest) {
            isVaultRequest = false;
            sendCheckoutRequest(context, (PayPalCheckoutRequest) payPalRequest, callback);
        } else if (payPalRequest instanceof PayPalVaultRequest) {
            isVaultRequest = true;
            sendVaultRequest(context, (PayPalVaultRequest) payPalRequest, callback);
        }
    }

    private void sendCheckoutRequest(final Context context,
                                     final PayPalCheckoutRequest payPalCheckoutRequest,
                                     final PayPalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.single-payment.selected", null, null, isVaultRequest);
        if (payPalCheckoutRequest.getShouldOfferPayLater()) {
            braintreeClient.sendAnalyticsEvent("paypal.single-payment.paylater.offered", null, null, isVaultRequest);
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (error != null) {
                callbackCreatePaymentAuthFailure(callback,
                        new PayPalPaymentAuthRequest.Failure(error));
                return;
            }

            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callbackCreatePaymentAuthFailure(callback,
                        new PayPalPaymentAuthRequest.Failure(configInvalidError));
                return;
            }

            sendPayPalRequest(context, payPalCheckoutRequest, callback);
        });

    }

    private void sendVaultRequest(final Context context,
                                  final PayPalVaultRequest payPalVaultRequest,
                                  final PayPalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.selected", null, null, isVaultRequest);
        if (payPalVaultRequest.getShouldOfferCredit()) {
            braintreeClient.sendAnalyticsEvent("paypal.billing-agreement.credit.offered", null, null, isVaultRequest);
        }

        braintreeClient.getConfiguration((configuration, error) -> {
            if (error != null) {
                callbackCreatePaymentAuthFailure(callback,
                        new PayPalPaymentAuthRequest.Failure(error));
                return;
            }

            if (payPalConfigInvalid(configuration)) {
                Exception configInvalidError = createPayPalError();
                callbackCreatePaymentAuthFailure(callback,
                        new PayPalPaymentAuthRequest.Failure(configInvalidError));
                return;
            }

            sendPayPalRequest(context, payPalVaultRequest, callback);
        });
    }

    private void sendPayPalRequest(final Context context, final PayPalRequest payPalRequest,
                                   final PayPalPaymentAuthCallback callback) {
        internalPayPalClient.sendRequest(context, payPalRequest, (payPalResponse, error) -> {
            if (payPalResponse != null) {
                payPalContextId = payPalResponse.getPairingId();
                try {
                    BrowserSwitchOptions options = buildBrowserSwitchOptions(payPalResponse);
                    payPalResponse.setBrowserSwitchOptions(options);
                    callback.onPayPalPaymentAuthRequest(
                            new PayPalPaymentAuthRequest.ReadyToLaunch(payPalResponse));
                } catch (JSONException exception) {
                    callbackCreatePaymentAuthFailure(callback,
                            new PayPalPaymentAuthRequest.Failure(exception));
                }
            } else {
                callbackCreatePaymentAuthFailure(callback,
                        new PayPalPaymentAuthRequest.Failure(error));
            }
        });
    }

    private BrowserSwitchOptions buildBrowserSwitchOptions(
            PayPalPaymentAuthRequestParams paymentAuthRequest) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put("approval-url", paymentAuthRequest.getApprovalUrl());
        metadata.put("success-url", paymentAuthRequest.getSuccessUrl());

        String paymentType =
                paymentAuthRequest.isBillingAgreement() ? "billing-agreement" : "single-payment";

        metadata.put("payment-type", paymentType);
        metadata.put("client-metadata-id", paymentAuthRequest.getClientMetadataId());
        metadata.put("merchant-account-id", paymentAuthRequest.getMerchantAccountId());
        metadata.put("source", "paypal-browser");
        metadata.put("intent", paymentAuthRequest.getIntent());

        return new BrowserSwitchOptions().requestCode(BraintreeRequestCodes.PAYPAL)
                .appLinkUri(braintreeClient.getAppLinkReturnUri())
                .url(Uri.parse(paymentAuthRequest.getApprovalUrl()))
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .metadata(metadata);
    }

    /**
     * After receiving a result from the PayPal web authentication flow via
     * {@link PayPalLauncher#handleReturnToAppFromBrowser(PayPalPendingRequest.Started, Intent)},
     * pass the {@link PayPalPaymentAuthResult.Success} returned to this method to tokenize the PayPal
     * account and receive a {@link PayPalAccountNonce} on success.
     *
     * @param paymentAuthResult a {@link PayPalPaymentAuthResult.Success} received in the callback
     *                          from  {@link PayPalLauncher#handleReturnToAppFromBrowser(PayPalPendingRequest.Started, Intent)}
     * @param callback          {@link PayPalTokenizeCallback}
     */
    public void tokenize(@NonNull PayPalPaymentAuthResult.Success paymentAuthResult,
                         @NonNull final PayPalTokenizeCallback callback) {
        BrowserSwitchFinalResult.Success browserSwitchResult = paymentAuthResult.getPaymentAuthInfo().getBrowserSwitchResult();
        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String clientMetadataId = Json.optString(metadata, "client-metadata-id", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);
        String payPalIntent = Json.optString(metadata, "intent", null);
        String approvalUrl = Json.optString(metadata, "approval-url", null);
        String successUrl = Json.optString(metadata, "success-url", null);
        String paymentType = Json.optString(metadata, "payment-type", "unknown");

        boolean isBillingAgreement = paymentType.equalsIgnoreCase("billing-agreement");
        String tokenKey = isBillingAgreement ? "ba_token" : "token";

        if (approvalUrl != null) {
            String pairingId = Uri.parse(approvalUrl).getQueryParameter(tokenKey);
            if (pairingId != null && !pairingId.isEmpty()) {
                payPalContextId = pairingId;
            }
        }

        try {
            Uri deepLinkUri = browserSwitchResult.getReturnUrl();
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
                            if (payPalAccountNonce != null) {
                                callbackTokenizeSuccess(callback,
                                        new PayPalResult.Success(payPalAccountNonce));
                            } else if (error != null) {
                                callbackTokenizeFailure(callback,
                                        new PayPalResult.Failure(error));
                            }
                        });
            } else {
                callbackTokenizeFailure(callback,
                        new PayPalResult.Failure(new BraintreeException("Unknown error")));
            }
        } catch (UserCanceledException e) {
            callbackBrowserSwitchCancel(callback, PayPalResult.Cancel.INSTANCE);
        } catch (JSONException | PayPalBrowserSwitchException e) {
            callbackTokenizeFailure(callback, new PayPalResult.Failure(e));
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

    private void callbackCreatePaymentAuthFailure(PayPalPaymentAuthCallback callback,
                                                  PayPalPaymentAuthRequest.Failure failure) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, payPalContextId, null, isVaultRequest);
        callback.onPayPalPaymentAuthRequest(failure);
    }

    private void callbackBrowserSwitchCancel(PayPalTokenizeCallback callback,
                                             PayPalResult.Cancel cancel) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, payPalContextId, null, isVaultRequest);
        callback.onPayPalResult(cancel);
    }

    private void callbackTokenizeFailure(PayPalTokenizeCallback callback,
                                         PayPalResult.Failure failure) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, payPalContextId, null, isVaultRequest);
        callback.onPayPalResult(failure);
    }

    private void callbackTokenizeSuccess(PayPalTokenizeCallback callback,
                                         PayPalResult.Success success) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, payPalContextId, null, isVaultRequest);
        callback.onPayPalResult(success);
    }
}
