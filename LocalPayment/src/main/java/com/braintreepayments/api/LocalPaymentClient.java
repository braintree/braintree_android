package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to integrate with local payments.
 */
public class LocalPaymentClient {

    static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    static final String LOCAL_PAYMENT_SUCCESS = "local-payment-success";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;
    private final LocalPaymentApi localPaymentApi;

    public LocalPaymentClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector(), new LocalPaymentApi(braintreeClient));
    }

    @VisibleForTesting
    LocalPaymentClient(@NonNull BraintreeClient braintreeClient, @NonNull PayPalDataCollector payPalDataCollector, LocalPaymentApi localPaymentApi) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
        this.localPaymentApi = localPaymentApi;
    }

    /**
     * Prepares the payment flow for a specific type of local payment.
     *
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentStartCallback}
     */
    public void startPayment(@NonNull final LocalPaymentRequest request, @NonNull final LocalPaymentStartCallback callback) {
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
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        if (!configuration.isPayPalEnabled()) {
                            callback.onResult(null, new ConfigurationException("Local payments are not enabled for this merchant."));
                            return;
                        }

                        sendAnalyticsEvent(request.getPaymentType(), "local-payment.start-payment.selected");

                        localPaymentApi.createPaymentMethod(request, new LocalPaymentStartCallback() {
                            @Override
                            public void onResult(@Nullable LocalPaymentResult localPaymentResult, @Nullable Exception error) {
                                if (localPaymentResult != null) {
                                    sendAnalyticsEvent(request.getPaymentType(), "local-payment.create.succeeded");
                                } else if (error != null) {
                                    sendAnalyticsEvent(request.getPaymentType(), "local-payment.webswitch.initiate.failed");
                                }
                            }
                        });
                    } else {
                        callback.onResult(null, error);
                    }
                }
            });
        }
    }

    /**
     * Initiates the browser switch for a payment flow by opening a browser where the customer can authenticate with their bank.
     *
     * @param activity           Android FragmentActivity
     * @param localPaymentResult {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}
     *                           and now has an approvalUrl and paymentId.
     */
    public void approvePayment(@NonNull FragmentActivity activity, @NonNull LocalPaymentResult localPaymentResult) throws JSONException, BrowserSwitchException {
        //noinspection ConstantConditions
        if (activity == null) {
            throw new RuntimeException("A FragmentActivity is required.");
        }

        //noinspection ConstantConditions
        if (localPaymentResult == null) {
            throw new RuntimeException("A LocalPaymentTransaction is required.");
        }

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .url(Uri.parse(localPaymentResult.getApprovalUrl()));

        String paymentType = localPaymentResult.getRequest().getPaymentType();

        browserSwitchOptions.metadata(new JSONObject()
                .put("merchant-account-id", localPaymentResult.getRequest().getMerchantAccountId())
                .put("payment-type", localPaymentResult.getRequest().getPaymentType()));

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    /**
     * @param context             Android Contex
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback            {@link LocalPaymentBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(@NonNull final Context context, @NonNull BrowserSwitchResult browserSwitchResult, @NonNull final LocalPaymentBrowserSwitchResultCallback callback) {
        //noinspection ConstantConditions
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

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
                    callback.onResult(null, new BraintreeException("LocalPayment encountered an error, " +
                            "return URL is invalid."));
                    return;
                }

                String responseString = deepLinkUri.toString();
                if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
                    sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                    callback.onResult(null, new UserCanceledException("User canceled Local Payment."));
                    return;
                }

                localPaymentApi.tokenize(merchantAccountId, responseString, payPalDataCollector.getClientMetadataId(context), new LocalPaymentBrowserSwitchResultCallback() {
                    @Override
                    public void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error) {
                       if (localPaymentNonce != null) {
                           sendAnalyticsEvent(paymentType, "local-payment.tokenize.succeeded");
                       } else if (error != null) {
                           sendAnalyticsEvent(paymentType, "local-payment.tokenize.failed");
                       }
                    }
                });
        }
    }

    private void sendAnalyticsEvent(String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
