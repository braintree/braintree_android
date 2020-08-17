package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.LocalPaymentRequest;
import com.braintreepayments.api.models.LocalPaymentResult;
import com.paypal.android.sdk.data.collector.PayPalDataCollector;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to integrate with local payments.
 */
public class LocalPayment {

    private static final String LOCAL_PAYMENT_CANCEL = "local-payment-cancel";
    private static final String LOCAL_PAYMENT_SUCCESSS = "local-payment-success";

    private static String sMerchantAccountId;
    private static String sPaymentType;

    /**
     * Prepares the payment flow for a specific type of local payment.
     *
     * @param fragment {@link BraintreeFragment}
     * @param request  {@link LocalPaymentRequest} with the payment details.
     * @param listener {@link BraintreeResponseListener} the callback to which the {@link LocalPaymentRequest} will be sent
     *                 with a payment ID for preprocessing and an approval URL. Once ready, use {@link #approvePayment(BraintreeFragment, LocalPaymentRequest)}
     *                 to initiate the browser switch.
     */
    public static void startPayment(final BraintreeFragment fragment, final LocalPaymentRequest request,
                                    final BraintreeResponseListener<LocalPaymentRequest> listener) {
        if (request == null) {
            fragment.postCallback(new BraintreeException("A LocalPaymentRequest is required."));
            return;
        } else if (request.getApprovalUrl() != null || request.getPaymentId() != null) {
            fragment.postCallback(new BraintreeException("LocalPaymentRequest is invalid, " +
                    "appovalUrl and paymentId should not be set."));
            return;
        } else if (request.getPaymentType() == null || request.getAmount() == null) {
            fragment.postCallback(new BraintreeException("LocalPaymentRequest is invalid, " +
                    "paymentType and amount are required."));
            return;
        } else if (listener == null) {
            fragment.postCallback(new BraintreeException("BraintreeResponseListener<LocalPaymentRequest> " +
                    "is required."));
            return;
        }

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getPayPal().isEnabled()) {
                    fragment.postCallback(new ConfigurationException("Local payments are not enabled for this merchant."));
                    return;
                }

                sMerchantAccountId = request.getMerchantAccountId();
                sPaymentType = request.getPaymentType();
                String returnUrl = fragment.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_SUCCESSS;
                String cancel = fragment.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_CANCEL;

                fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.start-payment.selected");

                fragment.getHttpClient().post("/v1/local_payments/create", request.build(returnUrl, cancel),
                        new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                try {
                                    JSONObject responseJson = new JSONObject(responseBody);
                                    request.approvalUrl(responseJson.getJSONObject("paymentResource").getString("redirectUrl"));
                                    request.paymentId(responseJson.getJSONObject("paymentResource").getString("paymentToken"));

                                    fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.create.succeeded");
                                    listener.onResponse(request);

                                } catch (JSONException jsonException) {
                                    failure(jsonException);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.webswitch.initiate.failed");
                                fragment.postCallback(exception);
                            }
                        });
            }
        });
    }

    /**
     * Initiates the browser switch for a payment flow by opening a browser where the customer can authenticate with their bank.
     *
     * @param fragment {@link BraintreeFragment}
     * @param request  {@link LocalPaymentRequest} which has already been sent to {@link #startPayment(BraintreeFragment, LocalPaymentRequest, BraintreeResponseListener)}
     *                 and now has an approvalUrl and paymentId.
     */
    public static void approvePayment(BraintreeFragment fragment, LocalPaymentRequest request) {
        fragment.browserSwitch(BraintreeRequestCodes.LOCAL_PAYMENT, request.getApprovalUrl());
        fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.webswitch.initiate.succeeded");
    }

    static void onActivityResult(final BraintreeFragment fragment, int resultCode, Intent data) {

        if (resultCode == AppCompatActivity.RESULT_CANCELED) {
            postCancelCallbackAndSendAnalytics(fragment);
            return;
        }

        Uri uri = (data == null) ? null : data.getData();
        if (uri == null) {
            postErrorCallbackAndSendAnalytics(fragment);
            return;
        }

        String responseString = uri.toString();
        if (responseString.toLowerCase().contains(LOCAL_PAYMENT_CANCEL.toLowerCase())) {
            postCancelCallbackAndSendAnalytics(fragment);
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("merchant_account_id", sMerchantAccountId);

            JSONObject paypalAccount = new JSONObject()
                    .put("intent", "sale")
                    .put("response", new JSONObject().put("webURL", responseString))
                    .put("options", new JSONObject().put("validate", false))
                    .put("response_type", "web")
                    .put("correlation_id", PayPalDataCollector.getClientMetadataId(fragment.getApplicationContext()));
            payload.put("paypal_account", paypalAccount);

            JSONObject metaData = new JSONObject()
                    .put("source", "client")
                    .put("integration", fragment.getIntegrationType())
                    .put("sessionId", fragment.getSessionId());
            payload.put("_meta", metaData);

            fragment.getHttpClient().post("/v1/payment_methods/paypal_accounts", payload.toString(),
                    new HttpResponseCallback() {
                        @Override
                        public void success(String responseBody) {
                            try {
                                LocalPaymentResult result = LocalPaymentResult.fromJson(responseBody);
                                fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.tokenize.succeeded");
                                fragment.postCallback(result);
                            } catch (JSONException jsonException) {
                                failure(jsonException);
                            }
                        }

                        @Override
                        public void failure(Exception exception) {
                            fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.tokenize.failed");
                            fragment.postCallback(exception);
                        }
                    });

        } catch (JSONException ignored) { /* do nothing */ }
    }

    private static String paymentTypeForAnalytics() {
        return sPaymentType != null ? sPaymentType : "unknown";
    }

    private static void postCancelCallbackAndSendAnalytics(BraintreeFragment fragment) {
        fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.webswitch.canceled");
        fragment.postCancelCallback(BraintreeRequestCodes.LOCAL_PAYMENT);
    }

    private static void postErrorCallbackAndSendAnalytics(BraintreeFragment fragment) {
        fragment.sendAnalyticsEvent(paymentTypeForAnalytics() + ".local-payment.webswitch-response.invalid");
        fragment.postCallback(new BraintreeException("LocalPayment encountered an error, " +
                "return URL is invalid."));
    }
}
