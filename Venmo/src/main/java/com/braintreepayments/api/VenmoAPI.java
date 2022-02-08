package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class VenmoAPI {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    VenmoAPI(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    public void createPaymentContext(@NonNull final VenmoRequest request, String venmoProfileId, final VenmoApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("query", "mutation CreateVenmoPaymentContext($input: CreateVenmoPaymentContextInput!) { createVenmoPaymentContext(input: $input) { venmoPaymentContext { id } } }");
            JSONObject input = new JSONObject();
            input.put("paymentMethodUsage", request.getPaymentMethodUsageAsString());
            input.put("merchantProfileId", venmoProfileId);
            input.put("customerClient", "MOBILE_APP");
            input.put("intent", "CONTINUE");

            input.putOpt("displayName", request.getDisplayName());

            JSONObject variables = new JSONObject();
            variables.put("input", input);
            params.put("variables", variables);
        } catch (JSONException e) {
            callback.onResult(null, new BraintreeException("unexpected error"));
        }

        braintreeClient.sendGraphQLPOST(params.toString(), new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                if (responseBody != null) {
                    String paymentContextId = parsePaymentContextId(responseBody);
                    if (TextUtils.isEmpty(paymentContextId)) {
                        callback.onResult(null, new BraintreeException("Failed to fetch a Venmo paymentContextId while constructing the requestURL."));
                        return;
                    }
                    callback.onResult(paymentContextId,null);
                } else {
                    callback.onResult(null, httpError);
                }
            }
        });
    }

    public void createNonceFromPaymentContext(String paymentContextId, final VenmoOnActivityResultCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("query", "query PaymentContext($id: ID!) { node(id: $id) { ... on VenmoPaymentContext { paymentMethodId userName payerInfo { firstName lastName phoneNumber email externalId userName } } } }");
            JSONObject variables = new JSONObject();
            variables.put("id", paymentContextId);
            params.put("variables", variables);

            braintreeClient.sendGraphQLPOST(params.toString(), new HttpResponseCallback() {

                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            JSONObject data = new JSONObject(responseBody).getJSONObject("data");
                            VenmoAccountNonce nonce = VenmoAccountNonce.fromJSON(data.getJSONObject("node"));

                            callback.onResult(nonce, null);
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

    void vaultVenmoAccountNonce(String nonce, final VenmoOnActivityResultCallback callback) {
        VenmoAccount venmoAccount = new VenmoAccount();
        venmoAccount.setNonce(nonce);

        apiClient.tokenizeREST(venmoAccount, new TokenizeCallback() {
            @Override
            public void onResult(JSONObject tokenizationResponse, Exception exception) {
                if (tokenizationResponse != null) {
                    try {
                        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(tokenizationResponse);
                        callback.onResult(venmoAccountNonce, null);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, exception);
                }
            }
        });
    }

    private static String parsePaymentContextId(String createPaymentContextResponse) {
        String paymentContextId = null;
        try {
            JSONObject data = new JSONObject(createPaymentContextResponse).getJSONObject("data");
            JSONObject createVenmoPaymentContext = data.getJSONObject("createVenmoPaymentContext");
            JSONObject venmoPaymentContext = createVenmoPaymentContext.getJSONObject("venmoPaymentContext");
            paymentContextId = venmoPaymentContext.getString("id");
        } catch (JSONException ignored) { /* do nothing */ }

        return paymentContextId;
    }

}
