package com.braintreepayments.api;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;

/**
 * Test utility class for setting up DropIn for tests.
 */
public class DropInTestUtils {

    /**
     * Injects an instance of {@link com.braintreepayments.api.Braintree} that will use the given
     * client token and sets the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @param clientToken
     */
    public static void setUpActivityTest(
            ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase, String clientToken)
            throws JSONException {
        injectBraintree(getTargetContext(), clientToken, clientToken);
        setClientTokenExtraForTest(testCase, clientToken);
    }

    /**
     * Creates a {@link com.braintreepayments.api.models.ClientToken} with FakePayPal
     * and sets it the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @return
     */
    public static String setClientTokenExtraForTest(ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase) {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        setClientTokenExtraForTest(testCase, clientToken);

        return clientToken;
    }

    /**
     * Sets the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @param clientToken
     */
    public static void setClientTokenExtraForTest(ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase,
            String clientToken) {
        Intent intent = new Intent(testCase.getInstrumentation().getContext(),
                BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        testCase.setActivityIntent(intent);
    }

    /**
     * Post an unrecoverable error to the {@link com.braintreepayments.api.Braintree.ErrorListener}s
     * currently registered with the {@link Braintree} instance.
     *
     * @param braintree
     * @param exception
     */
    public static void postUnrecoverableErrorFromBraintree(Braintree braintree, BraintreeException exception) {
        braintree.postExceptionToListeners(exception);
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
        braintree.addListener(new Braintree.PaymentMethodCreatedListener() {
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
}
