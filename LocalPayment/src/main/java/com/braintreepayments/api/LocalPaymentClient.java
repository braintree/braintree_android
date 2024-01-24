package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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
     * @param callback {@link LocalPaymentInternalAuthRequestCallback}
     */
    public void createPaymentAuthRequest(@NonNull final LocalPaymentRequest request,
                                         @NonNull final LocalPaymentAuthCallback callback) {
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
            callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(exception));
        } else {
            braintreeClient.getConfiguration((configuration, error) -> {
                if (configuration != null) {
                    if (!configuration.isPayPalEnabled()) {
                        callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(new ConfigurationException(
                                "Local payments are not enabled for this merchant.")));
                        return;
                    }

                    sendAnalyticsEvent(request.getPaymentType(),
                            "local-payment.start-payment.selected");

                    localPaymentApi.createPaymentMethod(request,
                            (localPaymentResult, createPaymentMethodError) -> {
                                if (localPaymentResult != null) {
                                    buildBrowserSwitchOptions(localPaymentResult, callback);
                                    sendAnalyticsEvent(request.getPaymentType(),
                                            "local-payment.create.succeeded");
                                } else if (createPaymentMethodError != null) {
                                    callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(new BraintreeException("An error " +
                                            "occurred creating the local payment method.")));
                                    sendAnalyticsEvent(request.getPaymentType(),
                                            "local-payment.webswitch.initiate.failed");
                                }
                            });
                } else if (error != null) {
                    callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(error));
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

        String paymentType = localPaymentAuthRequestParams.getRequest().getPaymentType();

        try {
            browserSwitchOptions.metadata(new JSONObject()
                    .put("merchant-account-id",
                            localPaymentAuthRequestParams.getRequest().getMerchantAccountId())
                    .put("payment-type", localPaymentAuthRequestParams.getRequest().getPaymentType()));
        } catch (JSONException e) {
            callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.Failure(new BraintreeException("Error parsing local payment request")));
        }

        localPaymentAuthRequestParams.setBrowserSwitchOptions(browserSwitchOptions);
        callback.onLocalPaymentAuthRequest(new LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams));
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    /**
     * After receiving a result from the web authentication flow via
     * {@link LocalPaymentLauncher#handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started, Intent)}, pass the
     * {@link LocalPaymentAuthResult} returned to this method to tokenize the local
     * payment method and receive a {@link LocalPaymentNonce} on success.
     *
     * @param context                         Android Context
     * @param localPaymentAuthResult a {@link LocalPaymentAuthResult} received from
     *                              {@link LocalPaymentLauncher#handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started, Intent)}
     * @param callback                        {@link LocalPaymentInternalTokenizeCallback}
     */
    public void tokenize(@NonNull final Context context,
                         @Nullable LocalPaymentAuthResult localPaymentAuthResult,
                         @NonNull final LocalPaymentTokenizeCallback callback) {
        //noinspection ConstantConditions
        if (localPaymentAuthResult == null) {
            callback.onLocalPaymentResult(new LocalPaymentResult.Failure(new BraintreeException("LocalPaymentAuthResult " +
                    "cannot be null")));
            return;
        }

        BrowserSwitchResult browserSwitchResult =
                localPaymentAuthResult.getBrowserSwitchResult();
        if (browserSwitchResult == null && localPaymentAuthResult.getError() != null) {
            callback.onLocalPaymentResult(new LocalPaymentResult.Failure(localPaymentAuthResult.getError()));
            return;
        }

        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        final String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                callback.onLocalPaymentResult(LocalPaymentResult.Cancel.INSTANCE);
                return;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri == null) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch-response.invalid");
                    callback.onLocalPaymentResult(new LocalPaymentResult.Failure(
                            new BraintreeException("LocalPayment encountered an error, " +
                                    "return URL is invalid.")));
                    return;
                }

                final String responseString = deepLinkUri.toString();
                if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                    callback.onLocalPaymentResult(LocalPaymentResult.Cancel.INSTANCE);
                    return;
                }
                braintreeClient.getConfiguration((configuration, error) -> {
                    if (configuration != null) {
                        localPaymentApi.tokenize(merchantAccountId, responseString,
                                dataCollector.getClientMetadataId(context, configuration),
                                (localPaymentNonce, localPaymentError) -> {
                                    if (localPaymentNonce != null) {
                                        sendAnalyticsEvent(paymentType,
                                                "local-payment.tokenize.succeeded");
                                        callback.onLocalPaymentResult(new LocalPaymentResult.Success(localPaymentNonce));
                                    } else if (localPaymentError != null) {
                                        sendAnalyticsEvent(paymentType,
                                                "local-payment.tokenize.failed");
                                        callback.onLocalPaymentResult(new LocalPaymentResult.Failure(localPaymentError));
                                    }
                                });
                    } else if (error != null) {
                        callback.onLocalPaymentResult(new LocalPaymentResult.Failure(error));
                    }
                });
        }
    }

    private void sendAnalyticsEvent(String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
