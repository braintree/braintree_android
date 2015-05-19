package com.braintreepayments.api;

import android.content.Context;
import android.os.SystemClock;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

/**
 * Test utility class used to inject pre-configured instances of {@link Braintree} for use in
 * specific test cases.
 */
public class TestDependencyInjector {

    /**
     * Inject an instance of Braintree that will use the supplied client token as configuration
     * and sleep for *delay* before every network request.
     *
     * @param context
     * @param clientTokenString
     * @param delay
     */
    public static void injectSlowBraintree(Context context, String clientTokenString,
            final long delay) {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        Configuration configuration = Configuration.fromJson(clientTokenString);
        HttpRequest httpRequest = new HttpRequest(clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse get(String url) throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.get(url);
            }

            @Override
            public HttpResponse post(String url, String params)
                    throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.post(url, params);
            }
        };
        httpRequest.setBaseUrl(configuration.getClientApiUrl());

        injectBraintree(context, clientTokenString, clientToken, configuration, httpRequest);
    }

    /**
     * Inject an instance of Braintree that will use the supplied client token and sleep for
     * *delay* before every network request.
     *
     * *Note:* This instance of Braintree is not setup and will require a call to the configuration
     * endpoint.
     *
     * @param context
     * @param clientTokenString
     * @param delay
     */
    public static void injectSlowNonSetupBraintree(Context context, String clientTokenString,
            final long delay) {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        HttpRequest httpRequest = new HttpRequest(clientToken.getAuthorizationFingerprint()) {
            @Override
            public HttpResponse get(String url) throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.get(url);
            }

            @Override
            public HttpResponse post(String url, String params)
                    throws BraintreeException, ErrorWithResponse {
                SystemClock.sleep(delay);
                return super.post(url, params);
            }
        };

        injectBraintree(context, clientTokenString, clientToken, null, httpRequest);
    }

    /**
     * Inject a mocked or already setup Braintree for use in Drop-In.
     *
     * @param clientToken
     * @param braintree
     */
    public static void injectBraintree(String clientToken, Braintree braintree) {
        Braintree.sInstances.put(clientToken, braintree);
    }

    /**
     * Inject an instance of Braintree that will use the supplied BraintreeApi
     *
     * @param clientToken
     * @param braintreeApi
     */
    public static void injectBraintree(String clientToken, BraintreeApi braintreeApi) {
        new Braintree(clientToken, braintreeApi);
    }

    /**
     * Convenience method to inject an instance of Braintree that will use the supplied configuration.
     *
     * @param context
     * @param clientTokenString
     * @param configurationString
     * @return
     */
    public static Braintree injectBraintree(Context context, String clientTokenString,
            String configurationString) {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        Configuration configuration = Configuration.fromJson(configurationString);
        HttpRequest httpRequest = new HttpRequest(clientToken.getAuthorizationFingerprint());
        httpRequest.setBaseUrl(configuration.getClientApiUrl());

        return injectBraintree(context, clientTokenString, clientToken, configuration, httpRequest);
    }

    /**
     * Inject an instance of Braintree that will use the supplied configuration.
     *
     * @param context
     * @param clientTokenString
     * @param clientToken
     * @param configuration
     * @param httpRequest
     * @return
     */
    public static Braintree injectBraintree(Context context, String clientTokenString,
            ClientToken clientToken, Configuration configuration, HttpRequest httpRequest) {
        return new Braintree(clientTokenString,
                new BraintreeApi(context, clientToken, configuration, httpRequest));
    }
}
