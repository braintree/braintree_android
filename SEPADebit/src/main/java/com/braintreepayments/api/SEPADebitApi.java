package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class SEPADebitApi {

    // TODO: switch this to ApiClient when Sandbox is ready
    private final HttpClient httpClient;

    SEPADebitApi() {
        this(new HttpClient(getSocketFactory(), new BraintreeHttpResponseParser()));
    }

    @VisibleForTesting
    SEPADebitApi(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void createMandate(SEPADebitRequest sepaDebitRequest, Configuration configuration, final CreateMandateCallback callback) {
        HttpRequest httpRequest;
        try {
            httpRequest = buildHttpRequest(sepaDebitRequest, configuration);
            httpClient.sendRequest(httpRequest, new HttpResponseCallback() {
                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            CreateMandateResult result = parseResponse(responseBody);
                            callback.onResult(result, null);
                        } catch (JSONException e) {
                            callback.onResult(null, e);
                        }
                    } else if (httpError != null) {
                        callback.onResult(null, httpError);
                    }
                }
            });
        } catch (JSONException e) {
            callback.onResult(null, e);
        }
    }

    private CreateMandateResult parseResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONObject sepaDebitAccount = json.getJSONObject("message").getJSONObject("body").getJSONObject("sepaDebitAccount");
        String approvalUrl = sepaDebitAccount.getString("approvalUrl");
        String ibanLastFour = sepaDebitAccount.getString("ibanLastChars");
        String customerId = sepaDebitAccount.getString("customerId");
        String bankReferenceToken = sepaDebitAccount.getString("bankReferenceToken");
        String mandateType = sepaDebitAccount.getString("mandateType");

        return new CreateMandateResult(approvalUrl, ibanLastFour, customerId, bankReferenceToken, mandateType);
    }

    private HttpRequest buildHttpRequest(SEPADebitRequest sepaDebitRequest, Configuration configuration) throws JSONException {
        JSONObject sepaDebitData = new JSONObject()
            .putOpt("account_holder_name", sepaDebitRequest.getAccountHolderName())
            .putOpt("customer_id", sepaDebitRequest.getCustomerId())
            .putOpt("iban", sepaDebitRequest.getIban());

        if (sepaDebitRequest.getMandateType() != null) {
            sepaDebitData.putOpt("mandate_type", sepaDebitRequest.getMandateType().toString());
        }

        if (sepaDebitRequest.getBillingAddress() != null) {
            JSONObject billingAddress = new JSONObject()
                    .putOpt("address_line_1", sepaDebitRequest.getBillingAddress().getStreetAddress())
                    .putOpt("address_line_2", sepaDebitRequest.getBillingAddress().getExtendedAddress())
                    .putOpt("admin_area_1", sepaDebitRequest.getBillingAddress().getLocality())
                    .putOpt("admin_area_2", sepaDebitRequest.getBillingAddress().getRegion())
                    .putOpt("postal_code", sepaDebitRequest.getBillingAddress().getPostalCode())
                    .putOpt("country_code", sepaDebitRequest.getBillingAddress().getCountryCodeAlpha2());

            sepaDebitData.put("billing_address", billingAddress);
        }

        JSONObject requestData = new JSONObject()
                .put("sepa_debit", sepaDebitData)
                .put("cancel_url", "https://example.com") // TODO: FUTURE PR set this in browser switch flow
                .put("return_url", "https://example.com"); // TODO: FUTURE PR set this in browser switch flow

        if (sepaDebitRequest.getMerchantAccountId() != null) {
            requestData.put("merchant_account_id", sepaDebitRequest.getMerchantAccountId());
        } else {
            requestData.putOpt("merchant_account_id", configuration.getMerchantAccountId());
        }

        HttpRequest request = new HttpRequest()
                .baseUrl("http://10.0.2.2:3000/")
                .method("POST")
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Key", "development_testing_pwpp_multi_account_merchant")
                .path("merchants/pwpp_multi_account_merchant/client_api/v1/sepa_debit")
                .data(requestData.toString());
        return request;
    }

    // TODO: Remove this when feature is in sandbox and ApiClient can be used
    private static SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory(BraintreeGatewayCertificate.getCertInputStream());
        } catch (SSLException e) {
            return null;
        }
    }
}
