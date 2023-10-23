package com.braintreepayments.api;

import android.content.Context;
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

    @VisibleForTesting
    BrowserSwitchResult pendingBrowserSwitchResult;

    /**
     * Create a new instance of {@link LocalPaymentClient} using a {@link BraintreeClient}.
     *
     * @param braintreeClient a {@link BraintreeClient}
     */
    public LocalPaymentClient(@NonNull BraintreeClient braintreeClient) {
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
     * Prepares the payment flow for a specific type of local payment.
     *
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentStartCallback}
     */
    public void startPayment(@NonNull final LocalPaymentRequest request,
                             @NonNull final LocalPaymentStartCallback callback) {
        Exception exception = null;

        //noinspection ConstantConditions
        if (callback == null) {
            throw new RuntimeException("A LocalPaymentCallback is required.");
        }

        //noinspection ConstantConditions
        if (request == null) {
            exception = new BraintreeException("A LocalPaymentRequest is required.");
        } else if (request.getPaymentType() == null || request.getAmount() == null) {
            exception = new BraintreeException(
                    "LocalPaymentRequest is invalid, paymentType and amount are required.");
        }

        if (exception != null) {
            callback.onResult(null, exception);
        } else {
            braintreeClient.getConfiguration((configuration, error) -> {
                if (configuration != null) {
                    if (!configuration.isPayPalEnabled()) {
                        callback.onResult(null, new ConfigurationException(
                                "Local payments are not enabled for this merchant."));
                        return;
                    }

                    sendAnalyticsEvent(request.getPaymentType(),
                            "local-payment.start-payment.selected");

                    localPaymentApi.createPaymentMethod(request,
                            (localPaymentResult, createPaymentMethodError) -> {
                                if (localPaymentResult != null) {
                                    approvePayment(localPaymentResult, callback);
                                    sendAnalyticsEvent(request.getPaymentType(),
                                            "local-payment.create.succeeded");
                                } else if (createPaymentMethodError != null) {
                                    callback.onResult(null, new BraintreeException("An error " +
                                            "occurred creating the local payment method."));
                                    sendAnalyticsEvent(request.getPaymentType(),
                                            "local-payment.webswitch.initiate.failed");
                                }
                            });
                } else {
                    callback.onResult(null, error);
                }
            });
        }
    }

    void approvePayment(@NonNull LocalPaymentResult localPaymentResult,
                        @NonNull LocalPaymentStartCallback callback) {
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
                .url(Uri.parse(localPaymentResult.getApprovalUrl()));

        String paymentType = localPaymentResult.getRequest().getPaymentType();

        try {
            browserSwitchOptions.metadata(new JSONObject()
                    .put("merchant-account-id",
                            localPaymentResult.getRequest().getMerchantAccountId())
                    .put("payment-type", localPaymentResult.getRequest().getPaymentType()));
        } catch (JSONException e) {
            callback.onResult(null, new BraintreeException("Error parsing local payment request"));
        }

        localPaymentResult.setBrowserSwitchOptions(browserSwitchOptions);
        callback.onResult(localPaymentResult, null);
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    /**
     * Use this method with the manual browser switch integration pattern.
     *
     * @param context                         Android Context
     * @param localPaymentBrowserSwitchResult a {@link BrowserSwitchResult} with a
     *                                        {@link BrowserSwitchStatus}
     * @param callback                        {@link LocalPaymentBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull final Context context,
                                      @Nullable
                                      LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult,
                                      @NonNull
                                      final LocalPaymentBrowserSwitchResultCallback callback) {
        //noinspection ConstantConditions
        if (localPaymentBrowserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("LocalPaymentBrowserSwitchResult " +
                    "cannot be null"));
            return;
        }

        BrowserSwitchResult browserSwitchResult =
                localPaymentBrowserSwitchResult.getBrowserSwitchResult();
        if (browserSwitchResult == null && localPaymentBrowserSwitchResult.getError() != null) {
            callback.onResult(null, localPaymentBrowserSwitchResult.getError());
            return;
        }

        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        final String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                callback.onResult(null, new UserCanceledException("User canceled Local Payment."));
                return;
            case BrowserSwitchStatus.SUCCESS:
                Uri deepLinkUri = browserSwitchResult.getDeepLinkUrl();
                if (deepLinkUri == null) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch-response.invalid");
                    callback.onResult(null,
                            new BraintreeException("LocalPayment encountered an error, " +
                                    "return URL is invalid."));
                    return;
                }

                final String responseString = deepLinkUri.toString();
                if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                    callback.onResult(null,
                            new UserCanceledException("User canceled Local Payment."));
                    return;
                }
                braintreeClient.getConfiguration((configuration, error) -> {
                    if (configuration != null) {
                        localPaymentApi.tokenize(merchantAccountId, responseString,
                                dataCollector.getClientMetadataId(context, configuration),
                                (localPaymentNonce, error1) -> {
                                    if (localPaymentNonce != null) {
                                        sendAnalyticsEvent(paymentType,
                                                "local-payment.tokenize.succeeded");
                                    } else if (error1 != null) {
                                        sendAnalyticsEvent(paymentType,
                                                "local-payment.tokenize.failed");
                                    }
                                    callback.onResult(localPaymentNonce, error1);
                                });
                    } else if (error != null) {
                        callback.onResult(null, error);
                    }
                });
        }
    }

    private void sendAnalyticsEvent(String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
