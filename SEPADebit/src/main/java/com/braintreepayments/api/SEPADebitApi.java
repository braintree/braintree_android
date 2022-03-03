package com.braintreepayments.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class SEPADebitApi {

    // TODO: switch this to ApiClient when Sandbox is ready
    private final HttpClient httpClient;

    SEPADebitApi() {
        this.httpClient = new HttpClient(getSocketFactory(), new BraintreeHttpResponseParser());
    }

    void createMandate(SEPADebitRequest sepaDebitRequest, final CreateMandateCallback callback) {
        HttpRequest httpRequest;
        try {
            httpRequest = buildHttpRequest(sepaDebitRequest);
            httpClient.sendRequest(httpRequest, new HttpResponseCallback() {
                @Override
                public void onResult(String responseBody, Exception httpError) {
                    try {
                        CreateMandateResult result = parseResponse(responseBody);
                        callback.onResult(result, null);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
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
        String ibanLastFour = sepaDebitAccount.getString("ibaneLastChars");
        String customerId = sepaDebitAccount.getString("customerId");
        String bankReferenceToken = sepaDebitAccount.getString("bankReferenceToken");
        String mandateType = sepaDebitAccount.getString("mandateType");

        return new CreateMandateResult(approvalUrl, ibanLastFour, customerId, bankReferenceToken, mandateType);
    }

    private HttpRequest buildHttpRequest(SEPADebitRequest sepaDebitRequest) throws JSONException {
        // TODO: handle optional request params
        JSONObject billingAddress = new JSONObject()
                .putOpt("address_line_1", sepaDebitRequest.getBillingAddress().getStreetAddress())
                .putOpt("address_line_2", sepaDebitRequest.getBillingAddress().getExtendedAddress())
                .putOpt("admin_area_1", sepaDebitRequest.getBillingAddress().getLocality())
                .putOpt("admin_area_2", sepaDebitRequest.getBillingAddress().getRegion())
                .putOpt("postal_code", sepaDebitRequest.getBillingAddress().getPostalCode())
                .putOpt("country_code", sepaDebitRequest.getBillingAddress().getCountryCodeAlpha2());
        JSONObject sepaDebitData = new JSONObject()
                .putOpt("account_holder_name", sepaDebitRequest.getAccountHolderName())
                .putOpt("customer_id", sepaDebitRequest.getCustomerId())
                .put("iban", sepaDebitRequest.getIban())
                .put("mandate_type", sepaDebitRequest.getMandateType().toString())
                .put("billing_address", billingAddress);
        JSONObject requestData = new JSONObject()
                .put("sepa_debit", sepaDebitData)
                .put("cancel_url", "https://example.com")
                .put("return_url", "https://example.com")
                .put("merchant_account_id", "eur_pwpp_multi_account_merchant_account");

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
