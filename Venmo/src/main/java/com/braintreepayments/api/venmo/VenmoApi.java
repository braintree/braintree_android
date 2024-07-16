package com.braintreepayments.api.venmo;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.MetadataBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

class VenmoApi {

    private final BraintreeClient braintreeClient;
    private final ApiClient apiClient;

    VenmoApi(BraintreeClient braintreeClient, ApiClient apiClient) {
        this.braintreeClient = braintreeClient;
        this.apiClient = apiClient;
    }

    void createPaymentContext(@NonNull final VenmoRequest request, String venmoProfileId,
                              final VenmoApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("query",
                    "mutation CreateVenmoPaymentContext($input: CreateVenmoPaymentContextInput!) { createVenmoPaymentContext(input: $input) { venmoPaymentContext { id } } }");
            JSONObject input = new JSONObject();
            input.put("paymentMethodUsage", request.getPaymentMethodUsageAsString());
            input.put("merchantProfileId", venmoProfileId);
            input.put("customerClient", "MOBILE_APP");
            input.put("intent", "CONTINUE");
            input.put("isFinalAmount", request.getIsFinalAmountAsString());
            JSONObject paysheetDetails = new JSONObject();
            paysheetDetails.put("collectCustomerShippingAddress",
                    request.getCollectCustomerShippingAddressAsString());
            paysheetDetails.put("collectCustomerBillingAddress",
                    request.getCollectCustomerBillingAddressAsString());

            JSONObject transactionDetails = new JSONObject();
            transactionDetails.put("subTotalAmount", request.getSubTotalAmount());
            transactionDetails.put("discountAmount", request.getDiscountAmount());
            transactionDetails.put("taxAmount", request.getTaxAmount());
            transactionDetails.put("shippingAmount", request.getShippingAmount());
            transactionDetails.put("totalAmount", request.getTotalAmount());

            if (!request.getLineItems().isEmpty()) {
                JSONArray lineItems = new JSONArray();
                for (VenmoLineItem lineItem : request.getLineItems()) {
                    if (lineItem.getUnitTaxAmount() == null ||
                            lineItem.getUnitTaxAmount().equals("")) {
                        lineItem.setUnitTaxAmount("0");
                    }
                    lineItems.put(lineItem.toJson());
                }
                transactionDetails.put("lineItems", lineItems);
            }

            if (transactionDetails.length() > 0) {
                paysheetDetails.put("transactionDetails", transactionDetails);
            }
            input.put("paysheetDetails", paysheetDetails);

            input.putOpt("displayName", request.getDisplayName());

            JSONObject variables = new JSONObject();
            variables.put("input", input);
            params.put("variables", variables);

            JSONObject braintreeData = new MetadataBuilder()
                    .sessionId(braintreeClient.getSessionId())
                    .integration(braintreeClient.getIntegrationType())
                    .version()
                    .build();

            params.put("clientSdkMetadata", braintreeData);
        } catch (JSONException e) {
            callback.onResult(null, new BraintreeException("unexpected error"));
        }

        braintreeClient.sendGraphQLPOST(params, (responseBody, httpError) -> {
            if (responseBody != null) {
                String paymentContextId = parsePaymentContextId(responseBody);
                if (TextUtils.isEmpty(paymentContextId)) {
                    callback.onResult(null, new BraintreeException(
                            "Failed to fetch a Venmo paymentContextId while constructing the requestURL."));
                    return;
                }
                callback.onResult(paymentContextId, null);
            } else {
                callback.onResult(null, httpError);
            }
        });
    }

    void createNonceFromPaymentContext(String paymentContextId,
                                       final VenmoInternalCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("query",
                    "query PaymentContext($id: ID!) { node(id: $id) { ... on VenmoPaymentContext { paymentMethodId userName payerInfo { firstName lastName phoneNumber email externalId userName " +
                            "shippingAddress { fullName addressLine1 addressLine2 adminArea1 adminArea2 postalCode countryCode } billingAddress { fullName addressLine1 addressLine2 adminArea1 adminArea2 postalCode countryCode } } } } }");
            JSONObject variables = new JSONObject();
            variables.put("id", paymentContextId);
            params.put("variables", variables);

            braintreeClient.sendGraphQLPOST(params, (responseBody, httpError) -> {
                if (responseBody != null) {
                    try {
                        JSONObject data = new JSONObject(responseBody).getJSONObject("data");
                        VenmoAccountNonce nonce =
                                VenmoAccountNonce.fromJSON(data.getJSONObject("node"));

                        callback.onResult(nonce, null);
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
    }

    void vaultVenmoAccountNonce(String nonce, final VenmoInternalCallback callback) {
        VenmoAccount venmoAccount = new VenmoAccount();
        venmoAccount.setNonce(nonce);

        apiClient.tokenizeREST(venmoAccount, (tokenizationResponse, exception) -> {
            if (tokenizationResponse != null) {
                try {
                    VenmoAccountNonce venmoAccountNonce =
                            VenmoAccountNonce.fromJSON(tokenizationResponse);
                    callback.onResult(venmoAccountNonce, null);
                } catch (JSONException e) {
                    callback.onResult(null, e);
                }
            } else {
                callback.onResult(null, exception);
            }
        });
    }

    private static String parsePaymentContextId(String createPaymentContextResponse) {
        String paymentContextId = null;
        try {
            JSONObject data = new JSONObject(createPaymentContextResponse).getJSONObject("data");
            JSONObject createVenmoPaymentContext = data.getJSONObject("createVenmoPaymentContext");
            JSONObject venmoPaymentContext =
                    createVenmoPaymentContext.getJSONObject("venmoPaymentContext");
            paymentContextId = venmoPaymentContext.getString("id");
        } catch (JSONException ignored) { /* do nothing */ }

        return paymentContextId;
    }

}
