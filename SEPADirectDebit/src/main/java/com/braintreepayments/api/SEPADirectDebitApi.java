package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class SEPADirectDebitApi {

    // TODO: switch this to ApiClient when Sandbox is ready
    private final HttpClient httpClient;

    SEPADirectDebitApi() {
        this(new HttpClient(getSocketFactory(), new BraintreeHttpResponseParser()));
    }

    @VisibleForTesting
    SEPADirectDebitApi(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void createMandate(SEPADirectDebitRequest sepaDirectDebitRequest, Configuration configuration, String returnUrlScheme, final CreateMandateCallback callback) {
        HttpRequest httpRequest;
        try {
            httpRequest = buildCreateMandateHttpRequest(sepaDirectDebitRequest, configuration, returnUrlScheme);
            httpClient.sendRequest(httpRequest, new HttpResponseCallback() {
                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            CreateMandateResult result = parseCreateMandateResponse(responseBody);
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

    void tokenize(String ibanLastFour, String customerId, String bankReferenceToken, String mandateType, final SEPADirectDebitTokenizeCallback callback) {
        try {
            HttpRequest httpRequest = buildTokenizeHttpRequest(ibanLastFour, customerId, bankReferenceToken, mandateType);
            httpClient.sendRequest(httpRequest, new HttpResponseCallback() {
                @Override
                public void onResult(String responseBody, Exception httpError) {
                   if (responseBody != null) {
                       try {
                           SEPADirectDebitNonce nonce = parseTokenizeResponse(responseBody);
                           callback.onResult(nonce, null);
                       } catch (JSONException e) {
                           // TODO: handle error
                           e.printStackTrace();
                       }
                   }
                }
            });
        } catch (JSONException e) {
            // TODO: callback error
            e.printStackTrace();
        }
    }

    private SEPADirectDebitNonce parseTokenizeResponse(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);
        return SEPADirectDebitNonce.fromJSON(jsonResponse);
    }

    private HttpRequest buildTokenizeHttpRequest(String ibanLastFour, String customerId, String bankReferenceToken, String mandateType) throws JSONException {
        JSONObject accountData = new JSONObject()
                .put("iban_last_chars", ibanLastFour)
                .put("customer_id", customerId)
                .put("bank_reference_token", bankReferenceToken)
                .put("mandate_type", mandateType);
        JSONObject requestData = new JSONObject()
                .put("sepa_debit_account", accountData);

        return new HttpRequest()
                .baseUrl("http://10.0.2.2:3000/")
                .method("POST")
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Key", "development_testing_pwpp_multi_account_merchant")
                .path("merchants/pwpp_multi_account_merchant/client_api/v1/payment_methods/sepa_debit_accounts")
                .data(requestData.toString());
    }

    private CreateMandateResult parseCreateMandateResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONObject sepaDebitAccount = json.getJSONObject("message").getJSONObject("body").getJSONObject("sepaDebitAccount");
        String approvalUrl = sepaDebitAccount.getString("approvalUrl");
        String ibanLastFour = sepaDebitAccount.getString("ibanLastChars");
        String customerId = sepaDebitAccount.getString("customerId");
        String bankReferenceToken = sepaDebitAccount.getString("bankReferenceToken");
        String mandateType = sepaDebitAccount.getString("mandateType");

        return new CreateMandateResult(approvalUrl, ibanLastFour, customerId, bankReferenceToken, mandateType);
    }

    private HttpRequest buildCreateMandateHttpRequest(SEPADirectDebitRequest sepaDirectDebitRequest, Configuration configuration, String returnUrlScheme) throws JSONException {
        JSONObject sepaDebitData = new JSONObject()
            .putOpt("account_holder_name", sepaDirectDebitRequest.getAccountHolderName())
            .putOpt("customer_id", sepaDirectDebitRequest.getCustomerId())
            .putOpt("iban", sepaDirectDebitRequest.getIban());

        if (sepaDirectDebitRequest.getMandateType() != null) {
            sepaDebitData.putOpt("mandate_type", sepaDirectDebitRequest.getMandateType().toString());
        }

        if (sepaDirectDebitRequest.getBillingAddress() != null) {
            JSONObject billingAddress = new JSONObject()
                    .putOpt("address_line_1", sepaDirectDebitRequest.getBillingAddress().getStreetAddress())
                    .putOpt("address_line_2", sepaDirectDebitRequest.getBillingAddress().getExtendedAddress())
                    .putOpt("admin_area_1", sepaDirectDebitRequest.getBillingAddress().getLocality())
                    .putOpt("admin_area_2", sepaDirectDebitRequest.getBillingAddress().getRegion())
                    .putOpt("postal_code", sepaDirectDebitRequest.getBillingAddress().getPostalCode())
                    .putOpt("country_code", sepaDirectDebitRequest.getBillingAddress().getCountryCodeAlpha2());

            sepaDebitData.put("billing_address", billingAddress);
        }

        String cancelUrl = String.format("%s://sepa/cancel", returnUrlScheme);
        String successUrl = String.format("%s://sepa/success", returnUrlScheme);

        JSONObject requestData = new JSONObject()
                .put("sepa_debit", sepaDebitData)
                .put("cancel_url", cancelUrl)
                .put("return_url", successUrl);

        if (sepaDirectDebitRequest.getMerchantAccountId() != null) {
            requestData.put("merchant_account_id", sepaDirectDebitRequest.getMerchantAccountId());
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
