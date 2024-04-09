package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.sharedutils.Json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to integrate with local payments.
 */
public class LocalPaymentClient {

    static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    static final String LOCAL_PAYMENT_SUCCESS = "local-payment-success";

    private final BraintreeClient braintreeClient;
    private final DataCollector dataCollector;
    private final LocalPaymentApi localPaymentApi;

    /**
     * Used for linking events from the client to server side request
     * In the Local Payment flow this will be a Payment Token/Order ID
     */
    private String payPalContextId = null;

    /**
     * Initializes a new {@link LocalPaymentClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public LocalPaymentClient(@NonNull Context context, @NonNull String authorization) {
        this(new BraintreeClient(context, authorization));
    }

   @VisibleForTesting
    LocalPaymentClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new DataCollector(braintreeClient),
                new LocalPaymentApi(braintreeClient));
    }

    @VisibleForTesting
    LocalPaymentClient(@NonNull BraintreeClient braintreeClient,
                       @NonNull DataCollector dataCollector,
                       @NonNull LocalPaymentApi localPaymentApi) {
        this.braintreeClient = braintreeClient;
        this.dataCollector = dataCollector;
        this.localPaymentApi = localPaymentApi;
    }

    /**
     * Starts the payment flow for a {@link LocalPaymentRequest} and calls back a
     * {@link LocalPaymentAuthRequestParams} on success that should be used to launch the user
     * authentication flow.
     *
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentAuthCallback}
     */
    public void createPaymentAuthRequest(@NonNull final LocalPaymentRequest request,
                                         @NonNull final LocalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED);

        Exception exception = null;

        //noinspection ConstantConditions
        if (callback == null) {
            throw new RuntimeException("A LocalPaymentAuthRequestCallback is required.");
        }

        //noinspection ConstantConditions
        if (request == null) {
            exception = new BraintreeException("A LocalPaymentRequest is required.");
        } else if (request.getPaymentType() == null || request.getAmount() == null) {
            exception = new BraintreeException(
                    "LocalPaymentRequest is invalid, paymentType and amount are required.");
        }

        if (exception != null) {
            authRequestFailure(exception, callback);
        } else {
            braintreeClient.getConfiguration((configuration, error) -> {
                if (configuration != null) {
                    if (!configuration.isPayPalEnabled()) {
                        authRequestFailure(
                            new ConfigurationException("Local payments are not enabled for this " +
                                "merchant."),
                            callback
                        );
                        return;
                    }

                    localPaymentApi.createPaymentMethod(request,
                            (localPaymentResult, createPaymentMethodError) -> {
                                if (localPaymentResult != null) {
                                    String pairingId = localPaymentResult.getPaymentId();
                                    if (pairingId != null && !pairingId.isEmpty()) {
                                        payPalContextId = pairingId;
                                    }
                                    buildBrowserSwitchOptions(localPaymentResult, callback);
                                } else if (createPaymentMethodError != null) {
                                    authRequestFailure(
                                        new BraintreeException("An error occurred creating the " +
                                            "local payment method."),
                                        callback
                                    );
                                }
                            });
                } else if (error != null) {
                    authRequestFailure(error, callback);
                }
            });
        }
    }

    void buildBrowserSwitchOptions(@NonNull
                                   LocalPaymentAuthRequestParams localPaymentAuthRequestParams,
                                   @NonNull LocalPaymentAuthCallback callback) {
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .url(Uri.parse(localPaymentAuthRequestParams.getApprovalUrl()));

        try {
            browserSwitchOptions.metadata(new JSONObject()
                    .put("merchant-account-id",
                            localPaymentAuthRequestParams.getRequest().getMerchantAccountId())
                    .put("payment-type", localPaymentAuthRequestParams.getRequest().getPaymentType()));
        } catch (JSONException e) {
            authRequestFailure(
                new BraintreeException("Error parsing local payment request"),
                callback
            );
            return;
        }

        localPaymentAuthRequestParams.setBrowserSwitchOptions(browserSwitchOptions);
        callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams));
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, payPalContextId);
    }

    private void authRequestFailure(Exception error, LocalPaymentAuthCallback callback) {
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, payPalContextId);
        callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(error));
    }

    /**
     * After receiving a result from the web authentication flow via
     * {@link LocalPaymentLauncher#handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started, Intent)}, pass the
     * {@link LocalPaymentAuthResult.Success} returned to this method to tokenize the local
     * payment method and receive a {@link LocalPaymentNonce} on success.
     *
     * @param context                         Android Context
     * @param localPaymentAuthResult a {@link LocalPaymentAuthResult.Success} received from
     *                              {@link LocalPaymentLauncher#handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started, Intent)}
     * @param callback                        {@link LocalPaymentInternalTokenizeCallback}
     */
    public void tokenize(@NonNull final Context context,
                         @NonNull LocalPaymentAuthResult.Success localPaymentAuthResult,
                         @NonNull final LocalPaymentTokenizeCallback callback) {

        BrowserSwitchResultInfo browserSwitchResult = localPaymentAuthResult.getPaymentAuthInfo().getBrowserSwitchResultInfo();

        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        final String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
        if (deepLinkUri == null) {
            tokenizeFailure(
                new BraintreeException("LocalPayment encountered an error, return URL is " +
                    "invalid."),
                callback
            );
            return;
        }

        final String responseString = deepLinkUri.toString();
        if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
            callbackCancel(callback);
            return;
        }
        braintreeClient.getConfiguration((configuration, error) -> {
            if (configuration != null) {
                localPaymentApi.tokenize(merchantAccountId, responseString,
                        dataCollector.getClientMetadataId(context, configuration),
                        (localPaymentNonce, localPaymentError) -> {
                            if (localPaymentNonce != null) {
                                braintreeClient.sendAnalyticsEvent(
                                    LocalPaymentAnalytics.PAYMENT_SUCCEEDED, payPalContextId
                                );
                                callback.onLocalPaymentResult(new LocalPaymentResult.Success(localPaymentNonce));
                            } else if (localPaymentError != null) {
                                tokenizeFailure(localPaymentError, callback);
                            }
                        });
            } else if (error != null) {
                tokenizeFailure(error, callback);
            }
        });
    }

    private void callbackCancel(LocalPaymentTokenizeCallback callback){
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_CANCELED, payPalContextId);
        callback.onLocalPaymentResult(LocalPaymentResult.Cancel.INSTANCE);
    }
    private void tokenizeFailure(Exception error, LocalPaymentTokenizeCallback callback) {
        braintreeClient.sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, payPalContextId);
        callback.onLocalPaymentResult(new LocalPaymentResult.Failure(error));
    }
}
