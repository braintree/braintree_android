package com.braintreepayments.api.internal;

import android.text.TextUtils;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.BraintreeApiErrorResponse;
import com.braintreepayments.api.exceptions.UnprocessableEntityException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

/**
 * Network request class that handles BraintreeApi request specifics and threading.
 */
public class BraintreeApiHttpClient extends HttpClient {

    public static final String API_VERSION_2016_10_07 = "2016-10-07";

    private final String mBearer;
    private final String mApiVersion;

    public BraintreeApiHttpClient(String baseUrl, String bearer) {
        this(baseUrl, bearer, API_VERSION_2016_10_07);
    }

    public BraintreeApiHttpClient(String baseUrl, String bearer, String apiVersion) {
        super();

        mBaseUrl = baseUrl;
        mBearer = bearer;
        mApiVersion = apiVersion;

        setUserAgent("braintree/android/" + BuildConfig.VERSION_NAME);

        try {
            setSSLSocketFactory(new TLSSocketFactory(BraintreeApiCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    @Override
    protected HttpURLConnection init(String url) throws IOException {
        HttpURLConnection connection = super.init(url);

        if (!TextUtils.isEmpty(mBearer)) {
            connection.setRequestProperty("Authorization", "Bearer " + mBearer);
        }

        connection.setRequestProperty("Braintree-Version", mApiVersion);

        return connection;
    }

    @Override
    protected String parseResponse(HttpURLConnection connection) throws Exception {
        try {
            return super.parseResponse(connection);
        } catch (UnprocessableEntityException e) {
            throw new BraintreeApiErrorResponse(e.getMessage());
        }
    }
}
