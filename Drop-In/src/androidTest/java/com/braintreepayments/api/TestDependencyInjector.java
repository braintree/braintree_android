package com.braintreepayments.api;

import android.content.Context;
import android.os.SystemClock;

import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;

import java.util.concurrent.CountDownLatch;

/**
 * Test utility class used to inject pre-configured instances of {@link Braintree} for use in
 * specific test cases.
 */
public class TestDependencyInjector {

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
     * Create and setup {@link Braintree} synchronously for test.
     *
     * @param context
     * @param clientToken
     * @return {@link Braintree}
     */
    public static Braintree injectBraintree(Context context, String clientToken)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Braintree[] setupBraintree = new Braintree[1];
        Braintree.setup(context, clientToken, new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                if (setupSuccessful) {
                    setupBraintree[0] = braintree;
                    latch.countDown();
                }
            }
        });

        latch.await();

        return setupBraintree[0];
    }

    /**
     * Create a setup {@link Braintree}.
     *
     * @param context
     * @param clientToken
     * @param configuration
     * @return {@link Braintree}
     * @throws JSONException
     */
    public static Braintree injectBraintree(Context context, String clientToken, String configuration)
            throws JSONException {
        return new Braintree(context, clientToken, configuration);
    }


    /**
     * Inject an instance of Braintree that will use the supplied client token as configuration
     * and sleep for *delay* before every network request.
     *
     * @param context
     * @param clientTokenString
     * @param delay
     */
    public static void injectSlowBraintree(Context context, String clientTokenString,
            final long delay) throws JSONException {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        Configuration configuration = Configuration.fromJson(clientTokenString);

        BraintreeHttpClient httpRequest = getSlowHttpRequest(clientToken, delay);
        httpRequest.setBaseUrl(configuration.getClientApiUrl());

        Braintree braintree = new Braintree(context, clientTokenString, clientTokenString);
        braintree.mHttpClient = httpRequest;
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
            final long delay) throws JSONException {
        ClientToken clientToken = ClientToken.fromString(clientTokenString);
        BraintreeHttpClient httpRequest = getSlowHttpRequest(clientToken, delay);

        Braintree braintree = new Braintree(context, clientTokenString);
        braintree.mHttpClient = httpRequest;
    }

    private static BraintreeHttpClient getSlowHttpRequest(ClientToken clientToken, final long delay) {
        return new BraintreeHttpClient(clientToken.getAuthorizationFingerprint()) {
            @Override
            public void get(final String path, final HttpResponseCallback callback) {
                mThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(delay);
                        callGet(path, callback);
                    }
                });
            }

            private void callGet(String path, HttpResponseCallback callback) {
                super.get(path, callback);
            }

            @Override
            public void post(final String path, final String data, final HttpResponseCallback callback) {
                mThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(delay);
                        callPost(path, data, callback);
                    }
                });
            }

            private void callPost(String path, String data, HttpResponseCallback callback) {
                super.post(path, data, callback);
            }
        };
    }
}
