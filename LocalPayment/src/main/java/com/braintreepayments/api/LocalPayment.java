package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.LocalPaymentResult;

import org.json.JSONException;
import org.json.JSONObject;

// TODO: Rename class when API is finalized
public class LocalPayment {

    private static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    private static final String LOCAL_PAYMENT_SUCCESSS = "local-payment-success";

    private final String returnUrlScheme;
    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;

    LocalPayment(@NonNull String returnUrlScheme, @NonNull BraintreeClient braintreeClient) {
        this(returnUrlScheme, braintreeClient, new PayPalDataCollector());
    }

    LocalPayment(@NonNull String returnUrlScheme, @NonNull BraintreeClient braintreeClient, @NonNull PayPalDataCollector payPalDataCollector) {
        this.returnUrlScheme = returnUrlScheme;
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
    }

    /**
     * Prepares the payment flow for a specific type of local payment.
     *
     * @param context  Android context
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param callback {@link LocalPaymentStartCallback} the callback to which the {@link LocalPaymentTransaction} will be sent
     *                 with a payment ID for preprocessing and an approval URL. Once ready, use {@link #approveTransaction(FragmentActivity, LocalPaymentTransaction)}
     *                 to initiate the browser switch.
     */
    public void startPayment(@NonNull final Context context, @NonNull final LocalPaymentRequest request, @NonNull final LocalPaymentStartCallback callback) {
        Exception exception = null;
        if (request == null) {
            exception = new BraintreeException("A LocalPaymentRequest is required.");
        } else if (request.getApprovalUrl() != null || request.getPaymentId() != null) {
            exception = new BraintreeException("LocalPaymentRequest is invalid, " +
                    "approvalUrl and paymentId should not be set.");
        } else if (request.getPaymentType() == null || request.getAmount() == null) {
            exception = new BraintreeException("LocalPaymentRequest is invalid, " +
                    "paymentType and amount are required.");
        } else if (callback == null) {
            throw new RuntimeException("BraintreeResponseListener<LocalPaymentRequest> is required.");
        }

        if (exception != null) {
            callback.onResult(null, exception);
        } else {
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        if (!configuration.getPayPal().isEnabled()) {
                            callback.onResult(null, new ConfigurationException("Local payments are not enabled for this merchant."));
                            return;
                        }

                        String returnUrl = returnUrlScheme + "://" + LOCAL_PAYMENT_SUCCESSS;
                        String cancel = returnUrlScheme + "://" + LOCAL_PAYMENT_CANCEL;

                        sendAnalyticsEvent(context, request.getPaymentType(), "local-payment.start-payment.selected");

                        String url = "/v1/local_payments/create";
                        braintreeClient.sendPOST(url, request.build(returnUrl, cancel), new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                JSONObject responseJson = null;
                                try {
                                    responseJson = new JSONObject(responseBody);
                                    String redirectUrl = responseJson.getJSONObject("paymentResource").getString("redirectUrl");
                                    String paymentToken = responseJson.getJSONObject("paymentResource").getString("paymentToken");

                                    sendAnalyticsEvent(context, request.getPaymentType(), "local-payment.create.succeeded");
                                    LocalPaymentTransaction transaction = new LocalPaymentTransaction(request, redirectUrl, paymentToken);
                                    callback.onResult(transaction, null);
                                } catch (JSONException e) {
                                    failure(e);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                sendAnalyticsEvent(context, request.getPaymentType(), "local-payment.webswitch.initiate.failed");
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
     * @param activity    {@link FragmentActivity}
     * @param transaction {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(Context, LocalPaymentRequest, LocalPaymentStartCallback)}
     *                    and now has an approvalUrl and paymentId.
     */
    public void approveTransaction(@NonNull FragmentActivity activity, @NonNull LocalPaymentTransaction transaction) throws JSONException, BrowserSwitchException {
        BrowserSwitchOptions browserSwitchOptions = new BrowserSwitchOptions()
                .requestCode(BraintreeRequestCodes.LOCAL_PAYMENT)
                .url(Uri.parse(transaction.getApprovalUrl()));

        String paymentType = transaction.getRequest().getPaymentType();

        browserSwitchOptions.metadata(new JSONObject()
                .put("merchant-account-id", transaction.getRequest().getMerchantAccountId())
                .put("payment-type", transaction.getRequest().getPaymentType()));

        braintreeClient.startBrowserSwitch(activity, browserSwitchOptions);
        sendAnalyticsEvent(activity, paymentType, "local-payment.webswitch.initiate.succeeded");
    }

    public void onBrowserSwitchResult(final Context context, BrowserSwitchResult browserSwitchResult, @Nullable Uri uri, final LocalPaymentBrowserSwitchResultCallback callback) {
        JSONObject metadata = browserSwitchResult.getRequestMetadata();

        final String paymentType = Json.optString(metadata, "payment-type", null);
        String merchantAccountId = Json.optString(metadata, "merchant-account-id", null);

        int result = browserSwitchResult.getStatus();
        switch (result) {
            case BrowserSwitchResult.STATUS_CANCELED:
                sendAnalyticsEvent(context, paymentType, "local-payment.webswitch.canceled");
                callback.onResult(null, new BraintreeException("system canceled"));
                return;
            case BrowserSwitchResult.STATUS_OK:
                if (uri == null) {
                    sendAnalyticsEvent(context, paymentType, "local-payment.webswitch-response.invalid");
                    callback.onResult(null, new BraintreeException("LocalPayment encountered an error, " +
                            "return URL is invalid."));
                    return;
                }

                String responseString = uri.toString();
                if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
                    sendAnalyticsEvent(context, paymentType, "local-payment.webswitch.canceled");
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
                                LocalPaymentResult result = LocalPaymentResult.fromJson(responseBody);
                                sendAnalyticsEvent(context, paymentType, "local-payment.tokenize.succeeded");
                                callback.onResult(result, null);
                            } catch (JSONException jsonException) {
                                failure(jsonException);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            sendAnalyticsEvent(context, paymentType, "local-payment.tokenize.failed");
                            callback.onResult(null, exception);
                        }
                    });
                } catch (JSONException ignored) { /* do nothing */ }
        }
    }

    private void sendAnalyticsEvent(Context context, String paymentType, String eventSuffix) {
        String eventPrefix = (paymentType == null) ? "unknown" : paymentType;
        braintreeClient.sendAnalyticsEvent(String.format("%s.%s", eventPrefix, eventSuffix));
    }
}
