package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;

class PayPalInternalClient {

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";

    private static final String USER_ACTION_KEY = "useraction";

    private final String cancelUrl;
    private final String successUrl;

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;

    PayPalInternalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector());
    }

    @VisibleForTesting
    PayPalInternalClient(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;

        this.cancelUrl = String.format("%s://onetouch/v1/cancel", braintreeClient.getReturnUrlScheme());
        this.successUrl = String.format("%s://onetouch/v1/success", braintreeClient.getReturnUrlScheme());
    }

    void sendRequest(final Context context, final PayPalRequest payPalRequest, final PayPalInternalClientCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration == null) {
                    callback.onResult(null, error);
                    return;
                }
                try {
                    final boolean isBillingAgreement = payPalRequest instanceof PayPalVaultRequest;
                    String endpoint = isBillingAgreement
                            ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                    String url = String.format("/v1/%s", endpoint);

                    // TODO: call async getAuthorization here
                    String requestBody = payPalRequest.createRequestBody(configuration, braintreeClient.getAuthorization(), successUrl, cancelUrl);

                    braintreeClient.sendPOST(url, requestBody, new HttpResponseCallback() {

                        @Override
                        public void onResult(String responseBody, Exception httpError) {
                            if (responseBody != null) {
                                try {
                                    PayPalResponse payPalResponse = new PayPalResponse(payPalRequest)
                                            .successUrl(successUrl);

                                    PayPalPaymentResource paypalPaymentResource = PayPalPaymentResource.fromJson(responseBody);
                                    String redirectUrl = paypalPaymentResource.getRedirectUrl();
                                    if (redirectUrl != null) {
                                        Uri parsedRedirectUri = Uri.parse(redirectUrl);

                                        String pairingIdKey = isBillingAgreement ? "ba_token" : "token";
                                        String pairingId = parsedRedirectUri.getQueryParameter(pairingIdKey);
                                        String clientMetadataId = payPalRequest.getRiskCorrelationId() != null
                                                ? payPalRequest.getRiskCorrelationId() : payPalDataCollector.getClientMetadataId(context);

                                        if (pairingId != null) {
                                            payPalResponse
                                                    .pairingId(pairingId)
                                                    .clientMetadataId(clientMetadataId);
                                        }

                                        String approvalUrl = parsedRedirectUri
                                                .buildUpon()
                                                .appendQueryParameter(USER_ACTION_KEY, payPalResponse.getUserAction())
                                                .toString();
                                        payPalResponse.approvalUrl(approvalUrl);
                                    }
                                    callback.onResult(payPalResponse, null);

                                } catch (JSONException exception) {
                                    callback.onResult(null, exception);
                                }
                            } else {
                                callback.onResult(null, httpError);
                            }
                        }
                    });
                } catch (JSONException exception) {
                    callback.onResult(null, exception);
                }

            }
        });
    }
}
