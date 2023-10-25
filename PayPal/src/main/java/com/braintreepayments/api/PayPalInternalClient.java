package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

class PayPalInternalClient {

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT =
            "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT =
            "paypal_hermes/setup_billing_agreement";

    private final String cancelUrl;
    private final String successUrl;

    private final BraintreeClient braintreeClient;
    private final DataCollector dataCollector;
    private final ApiClient apiClient;

    PayPalInternalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new DataCollector(braintreeClient), new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalInternalClient(BraintreeClient braintreeClient, DataCollector dataCollector, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.dataCollector = dataCollector;
        this.apiClient = apiClient;

        this.cancelUrl =
                String.format("%s://onetouch/v1/cancel", braintreeClient.getReturnUrlScheme());
        this.successUrl =
                String.format("%s://onetouch/v1/success", braintreeClient.getReturnUrlScheme());
    }

    void sendRequest(final Context context, final PayPalRequest payPalRequest,
                     final PayPalInternalClientCallback callback) {
        braintreeClient.getAuthorization((authorization, authError) -> {
            if (authorization != null) {
                braintreeClient.getConfiguration((configuration, configError) -> {
                    if (configuration == null) {
                        callback.onResult(null, configError);
                        return;
                    }
                    try {
                        final boolean isBillingAgreement =
                                payPalRequest instanceof PayPalVaultRequest;
                        String endpoint = isBillingAgreement
                                ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                        String url = String.format("/v1/%s", endpoint);

                        String requestBody =
                                payPalRequest.createRequestBody(configuration, authorization,
                                        successUrl, cancelUrl);

                        braintreeClient.sendPOST(url, requestBody,
                                (responseBody, httpError) -> {
                                    if (responseBody != null) {
                                        try {
                                            PayPalResponse payPalResponse =
                                                    new PayPalResponse(payPalRequest)
                                                            .successUrl(successUrl);

                                            PayPalPaymentResource paypalPaymentResource =
                                                    PayPalPaymentResource.fromJson(responseBody);
                                            String redirectUrl =
                                                    paypalPaymentResource.getRedirectUrl();
                                            if (redirectUrl != null) {
                                                Uri parsedRedirectUri = Uri.parse(redirectUrl);

                                                String pairingIdKey =
                                                        isBillingAgreement ? "ba_token" : "token";
                                                String pairingId =
                                                        parsedRedirectUri.getQueryParameter(
                                                                pairingIdKey);
                                                String clientMetadataId =
                                                        payPalRequest.getRiskCorrelationId() != null
                                                                ?
                                                                payPalRequest.getRiskCorrelationId() :
                                                                dataCollector.getClientMetadataId(
                                                                        context, configuration);

                                                if (pairingId != null) {
                                                    payPalResponse
                                                            .pairingId(pairingId)
                                                            .clientMetadataId(clientMetadataId);
                                                }

                                                payPalResponse.approvalUrl(
                                                        parsedRedirectUri.toString());
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

    void tokenize(PayPalAccount payPalAccount, final PayPalBrowserSwitchResultCallback callback) {
        apiClient.tokenizeREST(payPalAccount, (tokenizationResponse, exception) -> {
            if (tokenizationResponse != null) {
                try {
                    PayPalAccountNonce payPalAccountNonce =
                            PayPalAccountNonce.fromJSON(tokenizationResponse);
                    callback.onResult(new Success(payPalAccountNonce));

                } catch (JSONException e) {
                    callback.onResult(new Failure(e));
                }
            } else {
                callback.onResult(new Failure(exception));
            }
        });
    }
}
