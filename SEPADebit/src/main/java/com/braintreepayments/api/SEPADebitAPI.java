package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

class SEPADebitAPI {

    // TODO: switch this to ApiClient when Sandbox is ready
    private final BraintreeHttpClient httpClient;

    SEPADebitAPI(BraintreeHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void createMandate(SEPADebitRequest request) {

        // construct HTTP request
        // make HTTP request
        // callback result
    }

    private HttpRequest buildHttpRequest(SEPADebitRequest sepaDebitRequest) throws JSONException {
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


}
