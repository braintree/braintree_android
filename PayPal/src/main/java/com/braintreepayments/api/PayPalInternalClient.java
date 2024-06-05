package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

class PayPalInternalClient {

    private static final String CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource";
    private static final String SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement";

    private final BraintreeClient braintreeClient;
    private final PayPalDataCollector payPalDataCollector;
    private final ApiClient apiClient;

    PayPalInternalClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new PayPalDataCollector(braintreeClient), new ApiClient(braintreeClient));
    }

    @VisibleForTesting
    PayPalInternalClient(BraintreeClient braintreeClient, PayPalDataCollector payPalDataCollector, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.payPalDataCollector = payPalDataCollector;
        this.apiClient = apiClient;
    }

    void sendRequest(final Context context, final PayPalRequest payPalRequest, final PayPalInternalClientCallback callback) {
        braintreeClient.getAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorizationResult(@Nullable final Authorization authorization, @Nullable Exception authError) {
                if (authorization != null) {
                    braintreeClient.getConfiguration(new ConfigurationCallback() {
                        @Override
                        public void onResult(@Nullable final Configuration configuration, @Nullable Exception configError) {
                            if (configuration == null) {
                                callback.onResult(null, configError);
                                return;
                            }
                            try {
                                final boolean isBillingAgreement = payPalRequest instanceof PayPalVaultRequest;
                                String endpoint = isBillingAgreement
                                        ? SETUP_BILLING_AGREEMENT_ENDPOINT : CREATE_SINGLE_PAYMENT_ENDPOINT;
                                String url = String.format("/v1/%s", endpoint);

                                String cancelUrl;
                                String successUrl;
                                if (payPalRequest.isAppLinkEnabled() && braintreeClient.getAppLinkReturnUri() != null) {
                                    String appLink = braintreeClient.getAppLinkReturnUri().toString();
                                    cancelUrl = String.format("%s/cancel", appLink);
                                    successUrl = String.format("%s/success", appLink);
                                } else {
                                    cancelUrl = String.format("%s://onetouch/v1/cancel", braintreeClient.getReturnUrlScheme());
                                    successUrl = String.format("%s://onetouch/v1/success", braintreeClient.getReturnUrlScheme());
                                }

                                String requestBody = payPalRequest.createRequestBody(configuration, authorization, successUrl, cancelUrl);

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
                                                    String pairingId = findPairingId(parsedRedirectUri);

                                                    String clientMetadataId = payPalRequest.getRiskCorrelationId();
                                                    if (clientMetadataId == null) {
                                                        PayPalDataCollectorInternalRequest dataCollectorRequest =
                                                                new PayPalDataCollectorInternalRequest(payPalRequest.hasUserLocationConsent())
                                                                        .setApplicationGuid(payPalDataCollector.getPayPalInstallationGUID(context));

                                                        if (pairingId != null) {
                                                            dataCollectorRequest.setRiskCorrelationId(pairingId);
                                                        }
                                                        clientMetadataId = payPalDataCollector.getClientMetadataId(context, dataCollectorRequest, configuration);
                                                    }

                                                    if (pairingId != null) {
                                                        payPalResponse.pairingId(pairingId);
                                                    }

                                                    payPalResponse
                                                            .clientMetadataId(clientMetadataId)
                                                            .approvalUrl(parsedRedirectUri.toString());
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
                } else {
                    callback.onResult(null, authError);
                }
            }
        });
    }

    void tokenize(PayPalAccount payPalAccount, final PayPalBrowserSwitchResultCallback callback) {
        apiClient.tokenizeREST(payPalAccount, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (tokenizationResponse != null) {
                    try {
                        PayPalAccountNonce payPalAccountNonce = PayPalAccountNonce.fromJSON(tokenizationResponse);
                        callback.onResult(payPalAccountNonce, null);

                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                }
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
