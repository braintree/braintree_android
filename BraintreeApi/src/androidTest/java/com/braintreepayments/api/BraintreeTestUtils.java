package com.braintreepayments.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.test.AbstractBraintreeListener;
import com.braintreepayments.testutils.FixturesHelper;

import org.json.JSONException;

import java.util.concurrent.CountDownLatch;

/**
 * Test utility class to setup specific configurations of {@link Braintree}.
 */
public class BraintreeTestUtils {

    public static void setUp(Context context) {
        BraintreeHttpClient.DEBUG = true;
        System.setProperty("dexmaker.dexcache", context.getCacheDir().getPath());
    }

    public static Configuration getConfigurationFromFixture(Context context, String fixture)
            throws JSONException {
        return Configuration.fromJson(FixturesHelper.stringFromFixture(context, fixture));
    }

    /**
     * Set {@link Braintree}'s {@link BraintreeHttpClient} to use a specific instance of
     * {@link BraintreeHttpClient}
     *
     * @param braintree
     * @param httpClient
     */
    public static void setBraintreeHttpClient(Braintree braintree, BraintreeHttpClient httpClient) {
        braintree.mHttpClient = httpClient;
    }

    public static BraintreeHttpClient httpClientWithExpectedError(final Exception exception) {
        return new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(exception);
            }

            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.failure(exception);
            }
        };
    }

    /**
     * Create and setup {@link Braintree} synchronously for test.
     *
     * @param context
     * @param clientToken
     * @return {@link Braintree}
     */
    public static Braintree getBraintree(Context context, String clientToken)
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
    public static Braintree getBraintree(Context context, String clientToken, String configuration)
            throws JSONException {
        return new Braintree(context, clientToken, configuration);
    }

    /**
     * Synchronously tokenize a {@link PaymentMethod} and return a nonce.
     *
     * @param braintree
     * @param paymentMethodBuilder
     * @return
     */
    public static String tokenize(Braintree braintree, PaymentMethodBuilder paymentMethodBuilder)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] nonce = new String[1];
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                nonce[0] = paymentMethodNonce;
                latch.countDown();
            }
        });

        braintree.tokenize(paymentMethodBuilder);
        latch.await();

        return nonce[0];
    }

    /**
     * Synchronously create a {@link PaymentMethod} and return a {@link PaymentMethod}.
     *
     * @param braintree
     * @param paymentMethodBuilder
     * @return
     */
    public static PaymentMethod create(Braintree braintree, PaymentMethodBuilder paymentMethodBuilder)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final PaymentMethod[] createdPaymentMethod = new PaymentMethod[1];
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                createdPaymentMethod[0] = paymentMethod;
                latch.countDown();
            }
        });

        braintree.create(paymentMethodBuilder);
        latch.await();

        return createdPaymentMethod[0];
    }

    public static void waitForMainThreadToFinish() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        latch.await();
    }
}
