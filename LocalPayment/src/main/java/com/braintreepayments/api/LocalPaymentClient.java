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

    private static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    private static final String LOCAL_PAYMENT_SUCCESSS = "local-payment-success";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;

    public LocalPaymentClient(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector());
    }

    @VisibleForTesting
    LocalPaymentClient(@NonNull BraintreeClient braintreeClient, @NonNull PayPalDataCollector payPalDataCollector) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
    }

    /**
     * Prepares the payment flow for a specific type of local payment.
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentStartCallback}
     */
    public void startPayment(@NonNull final LocalPaymentRequest request, @NonNull final LocalPaymentStartCallback callback) {
        Exception exception = null;
        if (request == null) {
            exception = new BraintreeException("A LocalPaymentRequest is required.");
        } else if (callback == null) {
            throw new RuntimeException("A LocalPaymentCallback is required.");
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

                        String returnUrl = braintreeClient.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_SUCCESSS;
                        String cancel = braintreeClient.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_CANCEL;

                        sendAnalyticsEvent(request.getPaymentType(), "local-payment.start-payment.selected");

                        String url = "/v1/local_payments/create";
                        braintreeClient.sendPOST(url, request.build(returnUrl, cancel), new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                try {
                                    JSONObject responseJson = new JSONObject(responseBody);
                                    String redirectUrl = responseJson.getJSONObject("paymentResource").getString("redirectUrl");
                                    String paymentToken = responseJson.getJSONObject("paymentResource").getString("paymentToken");

                                    sendAnalyticsEvent(request.getPaymentType(), "local-payment.create.succeeded");
                                    LocalPaymentTransaction transaction = new LocalPaymentTransaction(request, redirectUrl, paymentToken);
                                    callback.onResult(transaction, null);
                                } catch (JSONException e) {
                                    failure(e);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                sendAnalyticsEvent(request.getPaymentType(), "local-payment.webswitch.initiate.failed");
                                callback.onResult(null, exception);
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
     * @param activity    Android FragmentActivity
     * @param transaction {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}
     *                    and now has an approvalUrl and paymentId.
     */
    public void approveTransaction(@NonNull FragmentActivity activity, @NonNull LocalPaymentTransaction transaction) throws JSONException, BrowserSwitchException {
        if (activity == null) {
            throw new RuntimeException("A FragmentActivity is required.");
        } else if (transaction == null) {
            throw new RuntimeException("A LocalPaymentTransaction is required.");
        }

        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .returnUrlScheme(braintreeClient.getReturnUrlScheme())
                .url(Uri.parse(transaction.getApprovalUrl()));

        String paymentType = transaction.getRequest().getPaymentType();

        browserSwitchOptions.metadata(new JSONObject()
                .put("merchant-account-id", transaction.getRequest().getMerchantAccountId())
                .put("payment-type", transaction.getRequest().getPaymentType()));

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        sendAnalyticsEvent(paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    /**
     * @param context Android Contex
     * @param browserSwitchResult a {@link BrowserSwitchResult} with a {@link BrowserSwitchStatus}
     * @param callback {@link LocalPaymentBrowserSwitchResultCallback}
     */
    public void onBrowserSwitchResult(final Context context, BrowserSwitchResult browserSwitchResult, final LocalPaymentBrowserSwitchResultCallback callback) {
        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchStatus.CANCELED:
                sendAnalyticsEvent(paymentType, "local-payment.webswitch.canceled");
                callback.onResult(null, new BraintreeException("system canceled"));
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
                    callback.onResult(null, new BraintreeException("user canceled"));
                    return;
                }
                JSONObject payload = new JSONObject();

                try {
                    payload.put("merchant_account_id", merchantAccountId);

                    JSONObject paypalAccount = new JSONObject()
                            .put("intent", "sale")
                            .put("response", new JSONObject().put("webURL", responseString))
                            .put("options", new JSONObject().put("validate", false))
                            .put("response_type", "web")
                            .put("correlation_id", payPalDataCollector.getClientMetadataId(context));
                    payload.put("paypal_account", paypalAccount);

                    JSONObject metaData = new JSONObject()
                            .put("source", "client")
                            .put("integration", braintreeClient.getIntegrationType())
                            .put("sessionId", braintreeClient.getSessionId());
                    payload.put("_meta", metaData);

                    String url = "/v1/payment_methods/paypal_accounts";
                    braintreeClient.sendPOST(url, payload.toString(), new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                LocalPaymentNonce result = LocalPaymentNonce.fromJson(responseBody);
                                sendAnalyticsEvent(paymentType, "local-payment.tokenize.succeeded");
                                callback.onResult(result, null);
                            } catch (JSONException jsonException) {
                                failure(jsonException);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            sendAnalyticsEvent(paymentType, "local-payment.tokenize.failed");
                            callback.onResult(null, exception);
                        }
                    });
                } catch (JSONException ignored) { /* do nothing */ }
        }
    }

    private void sendAnalyticsEvent(String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
