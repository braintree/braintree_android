package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

public class BraintreeGraphQLHttpClient extends BraintreeApiHttpClient {

    private static final String API_VERSION_2018_01_08 = "2018-01-08";

    public BraintreeGraphQLHttpClient(String baseUrl, String authorization) {
        this(baseUrl, authorization, API_VERSION_2018_01_08);
    }

    private BraintreeGraphQLHttpClient(String baseUrl, String authorization, String apiVersion) {
        super(baseUrl, authorization, apiVersion);

        try {
            setSSLSocketFactory(new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    public void post(String data, HttpResponseCallback callback) {
        super.post("", data, callback);
    }

    @Override
    protected String parseResponse(HttpURLConnection connection) throws Exception {
        String response = super.parseResponse(connection);
        JSONArray errors = new JSONObject(response)
                .optJSONArray(ErrorWithResponse.GRAPHQL_ERRORS_KEY);

        if (errors != null) {
            ErrorWithResponse error = ErrorWithResponse.fromGraphQLJson(response);
            BraintreeError validateError = error.errorFor("validate");

            if (validateError != null) {
                throw new AuthorizationException(validateError.getMessage());
            } else {
                throw error;
            }
        }

        return response;
    }
}
