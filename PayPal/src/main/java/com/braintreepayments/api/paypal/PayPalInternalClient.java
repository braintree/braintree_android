package com.braintreepayments.api.paypal;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.datacollector.DataCollector;
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest;

import org.json.JSONException;

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
                        payPalRequest.createRequestBody(configuration,
                                braintreeClient.getAuthorization(),
                                successUrl, cancelUrl);

                braintreeClient.sendPOST(url, requestBody,
                        (responseBody, httpError) -> {
                            if (responseBody != null) {
                                try {
                                    PayPalPaymentAuthRequestParams paymentAuthRequest =
                                            new PayPalPaymentAuthRequestParams(payPalRequest)
                                                    .successUrl(successUrl);

                                    PayPalPaymentResource paypalPaymentResource =
                                            PayPalPaymentResource.fromJson(responseBody);
                                    String redirectUrl =
                                            paypalPaymentResource.getRedirectUrl();
                                    if (redirectUrl != null) {
                                        Uri parsedRedirectUri = Uri.parse(redirectUrl);

                                        String pairingId = findPairingId(parsedRedirectUri);
                                        String clientMetadataId = payPalRequest.getRiskCorrelationId();

                                        if (clientMetadataId == null) {
                                            DataCollectorInternalRequest dataCollectorRequest =
                                                new DataCollectorInternalRequest(payPalRequest.hasUserLocationConsent())
                                                    .setApplicationGuid(dataCollector.getPayPalInstallationGUID(context));

                                            if (pairingId != null) {
                                                dataCollectorRequest.setRiskCorrelationId(pairingId);
                                            }
                                            clientMetadataId = dataCollector.getClientMetadataId(context, dataCollectorRequest, configuration);
                                        }

                                        if (pairingId != null) {
                                            paymentAuthRequest.pairingId(pairingId);
                                        }

                                        paymentAuthRequest
                                            .clientMetadataId(clientMetadataId)
                                            .approvalUrl(parsedRedirectUri.toString());
                                    }
                                    callback.onResult(paymentAuthRequest, null);

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
    }

    void tokenize(PayPalAccount payPalAccount, final PayPalInternalTokenizeCallback callback) {
        apiClient.tokenizeREST(payPalAccount, (tokenizationResponse, exception) -> {
            if (tokenizationResponse != null) {
                try {
                    PayPalAccountNonce payPalAccountNonce =
                            PayPalAccountNonce.fromJSON(tokenizationResponse);
                    callback.onResult(payPalAccountNonce, null);

                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            } else {
                callback.onResult(null, exception);
            }
        });
    }

    private String findPairingId(Uri redirectUri) {
        String pairingId = redirectUri.getQueryParameter("ba_token");
        if (pairingId == null) {
            pairingId = redirectUri.getQueryParameter("token");
        }
        return pairingId;
    }
}
