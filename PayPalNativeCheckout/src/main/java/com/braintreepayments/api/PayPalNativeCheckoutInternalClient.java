package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;

class PayPalNativeCheckoutInternalClient {

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;
    private final ApiClient apiClient;

    interface PayPalNativeCheckoutInternalClientCallback {
        void onResult(@Nullable PayPalNativeCheckoutResponse payPalResponse, @Nullable Exception error);
    }

    PayPalNativeCheckoutInternalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector(), new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalNativeCheckoutInternalClient(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
        this.apiClient = apiClient;
    }

    void sendRequest(final Context context, final PayPalNativeRequest payPalRequest, final PayPalNativeCheckoutInternalClientCallback callback) {
        braintreeClient.getAuthorization((authorization, authError) -> {
            if (authorization != null) {
                braintreeClient.getConfiguration((configuration, configError) -> {
                    if (configuration == null) {
                        callback.onResult(null, configError);
                        return;
                    }
                    try {
                        final boolean isBillingAgreement = payPalRequest instanceof PayPalNativeCheckoutVaultRequest;
                        String endpoint = isBillingAgreement
                                ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                        String url = String.format("/v1/%s", endpoint);

                        String requestBody = payPalRequest.createRequestBody(configuration, authorization);

                        braintreeClient.sendPOST(url, requestBody, (responseBody, httpError) -> {
                            if (responseBody != null) {
                                try {
                                    PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalRequest);

                                    PayPalNativeCheckoutPaymentResource paypalPaymentResource = PayPalNativeCheckoutPaymentResource.fromJson(responseBody);
                                    String redirectUrl = paypalPaymentResource.getRedirectUrl();
                                    if (redirectUrl != null) {
                                        Uri parsedRedirectUri = Uri.parse(redirectUrl);

                                        String pairingIdKey = isBillingAgreement ? "ba_token" : "token";
                                        String pairingId = parsedRedirectUri.getQueryParameter(pairingIdKey);
                                        String clientMetadataId = payPalRequest.getRiskCorrelationId() != null
                                                ? payPalRequest.getRiskCorrelationId() : payPalDataCollector.getClientMetadataId(context, configuration);

                                        if (pairingId != null) {
                                            payPalResponse
                                                    .pairingId(pairingId)
                                                    .clientMetadataId(clientMetadataId);
                                        }
                                    }
                                    callback.onResult(payPalResponse, null);

                                } catch (JSONException exception) {
                                    callback.onResult(null, exception);
                                }
                            } else {
                                callback.onResult(null, httpError);
                            }
                        });
                    } catch (JSONException exception) {
                        callback.onResult(null, exception);
                    }
                });
            } else {
                callback.onResult(null, authError);
            }
        });
    }

    void tokenize(PayPalNativeCheckoutAccount payPalAccount, final PayPalNativeCheckoutResultCallback callback) {
        apiClient.tokenizeREST(payPalAccount, (tokenizationResponse, exception) -> {
            if (tokenizationResponse != null) {
                try {
                    PayPalNativeCheckoutAccountNonce payPalAccountNonce = PayPalNativeCheckoutAccountNonce.fromJSON(tokenizationResponse);
                    callback.onResult(payPalAccountNonce, null);

                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            } else {
                callback.onResult(null, exception);
            }
        });
    }
}
