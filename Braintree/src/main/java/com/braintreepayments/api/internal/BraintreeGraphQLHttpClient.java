package com.braintreepayments.api.internal;

import com.braintreepayments.api.Json;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

public class BraintreeGraphQLHttpClient extends BraintreeApiHttpClient {

    private static final String API_VERSION = "2018-03-06";

    public BraintreeGraphQLHttpClient(String baseUrl, String bearer) {
        this(baseUrl, bearer, API_VERSION);
    }

    private BraintreeGraphQLHttpClient(String baseUrl, String bearer, String apiVersion) {
        super(baseUrl, bearer, apiVersion);

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
            for (int i = 0; i < errors.length(); i++) {
                JSONObject error = errors.getJSONObject(i);
                JSONObject extensions = error.optJSONObject("extensions");

                if (extensions == null) {
                    throw new UnexpectedException("An unexpected error occurred");
                }

                if (Json.optString(extensions, "legacyCode", "").equals("50000")) {
                    throw new AuthorizationException(error.getString("message"));
                } else if (!Json.optString(extensions, "errorType", "").equals("user_error")) {
                    throw new UnexpectedException("An unexpected error occurred");
                }
            }

            throw ErrorWithResponse.fromGraphQLJson(response);
        }

        return response;
    }
}
