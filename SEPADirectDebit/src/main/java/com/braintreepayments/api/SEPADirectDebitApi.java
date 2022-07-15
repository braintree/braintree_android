package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class SEPADirectDebitApi {

    private final BraintreeClient braintreeClient;

    SEPADirectDebitApi(BraintreeClient braintreeClient) { this.braintreeClient = braintreeClient; }

    void createMandate(SEPADirectDebitRequest sepaDirectDebitRequest, String returnUrlScheme, final CreateMandateCallback callback) {
        try {
            JSONObject jsonObject = buildCreateMandateRequest(sepaDirectDebitRequest, returnUrlScheme);
            String url = "v1/sepa_debit";
            braintreeClient.sendPOST(url, jsonObject.toString(), new HttpResponseCallback() {

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
            JSONObject jsonObject = buildTokenizeRequest(ibanLastFour, customerId, bankReferenceToken, mandateType);
            String url = "v1/payment_methods/sepa_debit_accounts";
            braintreeClient.sendPOST(url, jsonObject.toString(), new HttpResponseCallback() {

                @Override
                public void onResult(String responseBody, Exception httpError) {
                   if (responseBody != null) {
                       try {
                           SEPADirectDebitNonce nonce = parseTokenizeResponse(responseBody);
                           callback.onResult(nonce, null);
                       } catch (JSONException jsonException) {
                           callback.onResult(null, jsonException);
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

    private SEPADirectDebitNonce parseTokenizeResponse(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);
        return SEPADirectDebitNonce.fromJSON(jsonResponse);
    }

    private JSONObject buildTokenizeRequest(String ibanLastFour, String customerId, String bankReferenceToken, String mandateType) throws JSONException {
        JSONObject accountData = new JSONObject()
                .put("last_4", ibanLastFour)
                .put("merchant_or_partner_customer_id", customerId)
                .put("bank_reference_token", bankReferenceToken)
                .put("mandate_type", mandateType);
        JSONObject requestData = new JSONObject()
                .put("sepa_debit_account", accountData);

        return requestData;
    }

    private CreateMandateResult parseCreateMandateResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONObject sepaDebitAccount = json.getJSONObject("message").getJSONObject("body").getJSONObject("sepaDebitAccount");
        String approvalUrl = sepaDebitAccount.getString("approvalUrl");
        String ibanLastFour = sepaDebitAccount.getString("last4");
        String customerId = sepaDebitAccount.getString("merchantOrPartnerCustomerId");
        String bankReferenceToken = sepaDebitAccount.getString("bankReferenceToken");
        String mandateType = sepaDebitAccount.getString("mandateType");

        return new CreateMandateResult(approvalUrl, ibanLastFour, customerId, bankReferenceToken, mandateType);
    }

    private JSONObject buildCreateMandateRequest(SEPADirectDebitRequest sepaDirectDebitRequest, String returnUrlScheme) throws JSONException {
        JSONObject sepaDebitData = new JSONObject()
            .putOpt("account_holder_name", sepaDirectDebitRequest.getAccountHolderName())
            .putOpt("merchant_or_partner_customer_id", sepaDirectDebitRequest.getCustomerId())
            .putOpt("iban", sepaDirectDebitRequest.getIban())
            .putOpt("mandate_type", sepaDirectDebitRequest.getMandateType().toString());

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
            requestData.putOpt("merchant_account_id", sepaDirectDebitRequest.getMerchantAccountId());
        }

        return requestData;
    }
}
