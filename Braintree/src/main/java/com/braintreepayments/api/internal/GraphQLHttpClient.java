package com.braintreepayments.api.internal;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

public class GraphQLHttpClient extends HttpClient {

    public static final String API_VERSION_2018_01_08 = "2018-01-08";

    private final Authorization mAuthorization;

    public GraphQLHttpClient(Authorization authorization) {
        mAuthorization = authorization;

        setUserAgent(getUserAgent());

        try {
            setSSLSocketFactory(new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    /**
     * @return User Agent {@link String} for the current SDK version.
     */
    public static String getUserAgent() {
        return "braintree/android/" + BuildConfig.VERSION_NAME;
    }

    public void post(String data, HttpResponseCallback callback) {
        super.post("", data, callback);
    }

    @Override
    protected HttpURLConnection init(String url) throws IOException {
        HttpURLConnection connection = super.init(url);

        connection.setRequestProperty("Authorization", "Bearer " + mAuthorization.getAuthorization());
        connection.setRequestProperty("Braintree-Version", API_VERSION_2018_01_08);

        return connection;
    }

    @Override
    protected String parseResponse(HttpURLConnection connection) throws Exception {
        String response = super.parseResponse(connection);

        JSONArray errors = new JSONObject(response).optJSONArray(ErrorWithResponse.GRAPHQL_ERRORS_KEY);
        if (errors != null) {
            ErrorWithResponse error = ErrorWithResponse.fromGraphQLJson(response);

            if (error.errorFor("validate") != null) {
                throw new AuthorizationException(error.errorFor("validate").getMessage());
            } else {
                throw error;
            }
        }

        return response;
    }
}
