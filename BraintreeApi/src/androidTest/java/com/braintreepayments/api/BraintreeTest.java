package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.ListenerCallback;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.Braintree.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;

import org.json.JSONException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BraintreeTest extends AndroidTestCase {

    Braintree mBraintree;
    BraintreeApi mBraintreeApi;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.setUp(getContext());
        mBraintreeApi = new BraintreeApi(getContext(), new TestClientTokenBuilder().build());
        mBraintree = new Braintree(mBraintreeApi);
    }

    public void testCreateAsync()
            throws BraintreeException, ExecutionException, InterruptedException, ErrorWithResponse {
        createCardSync(mBraintree);

        assertEquals(1, mBraintreeApi.getPaymentMethods().size());
    }

    public void testCreatePostsCardsToListeners() throws ExecutionException, InterruptedException {
        final AtomicBoolean paymentMethodCreatedCalled = new AtomicBoolean(false);
        final AtomicBoolean paymentMethodNonceCalled = new AtomicBoolean(false);
        mBraintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                paymentMethodCreatedCalled.set(true);
            }

            @Override
            public void onPaymentMethodNonce(String nonce) {
                assertNotNull(nonce);
                paymentMethodNonceCalled.set(true);
            }
        });

        createCardSync(mBraintree);

        TestUtils.waitForMainThreadToFinish();
        assertTrue(paymentMethodCreatedCalled.get());
        assertTrue(paymentMethodNonceCalled.get());
    }

    public void testTokenizePostsNonceToListeners() throws ExecutionException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        mBraintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                wasCalled.set(true);
            }
        });

        mBraintree.tokenizeHelper(new CardBuilder().cardNumber("55")).get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testListPaymentMethodsPostsCardsToListeners() throws ExecutionException,
            InterruptedException {
        createCardSync(mBraintree);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        mBraintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                fail("onPaymentMethodCreated should not be called for tokenizing");
            }

            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertTrue(paymentMethods.size() > 0);
                wasCalled.set(true);
            }
        });

        mBraintree.getPaymentMethodsHelper().get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testOnUnrecoverableErrorsPostsToListeners()
            throws ExecutionException, InterruptedException, UnexpectedException {
        BraintreeApi braintreeApi = TestUtils.unexpectedExceptionThrowingApi(getContext(),
                TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json"));
        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable != null);
                wasCalled.set(true);
            }
        });

        braintree.getPaymentMethodsHelper().get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.createHelper(new CardBuilder()).get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testOnRecoverableErrorsPostsToListeners()
            throws ExecutionException, InterruptedException, UnexpectedException {
        ClientToken clientToken =
                TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");
        String response = FixturesHelper.stringFromFixture(getContext(),
                "errors/error_response.json");

        BraintreeApi braintreeApi =
                TestUtils.apiWithExpectedResponse(getContext(), clientToken, response, 422);
        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertTrue(error != null);
                wasCalled.set(true);
            }
        });

        braintree.getPaymentMethodsHelper().get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.createHelper(new CardBuilder()).get();

        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testPayPalConfigurationExceptionPostsToOnUnrecoverableError()
            throws BraintreeException, InterruptedException, ErrorWithResponse {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.handlePayPalResponse(any(Activity.class), eq(PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID), eq(intent)))
                .thenThrow(new ConfigurationException());

        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable error) {
                assertTrue(error != null);
                wasCalled.set(true);
            }
        });

        braintree.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID, intent);
        SystemClock.sleep(50);
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.handlePayPalResponse(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID, intent);
        SystemClock.sleep(50);
        assertTrue(wasCalled.get());
    }

    public void testFinishPayWithPayPalDoesNothingOnNullBuilder() throws ConfigurationException {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.handlePayPalResponse(any(Activity.class), eq(PayPalFuturePaymentActivity.RESULT_CANCELED), eq(intent))).
                thenReturn(null);

        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean listenerWasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onUnrecoverableError(Throwable throwable) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                listenerWasCalled.set(true);
            }
        });

        braintree.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_CANCELED, intent);
        SystemClock.sleep(50);

        assertFalse("Expected no listeners to fire but one did fire", listenerWasCalled.get());
    }

    public void testFinishPayWithVenmoPostsNonceAndPaymentMethodOnSuccess()
            throws JSONException, BraintreeException, ErrorWithResponse {
        final PaymentMethod paymentMethod = new PaymentMethod() {
            @Override
            public String getTypeLabel() {
                return "I am a payment method";
            }
        };
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.finishPayWithVenmo(eq(Activity.RESULT_OK), any(Intent.class))).thenReturn(
                "nonce");
        when(braintreeApi.getPaymentMethod("nonce")).thenReturn(paymentMethod);

        Braintree braintree = new Braintree(braintreeApi);
        final AtomicBoolean nonceListenerCalled = new AtomicBoolean(false);
        final AtomicBoolean paymentMethodListenerCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod method) {
                paymentMethodListenerCalled.set(true);
                assertEquals(paymentMethod, method);
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                nonceListenerCalled.set(true);
                assertEquals("nonce", paymentMethodNonce);
            }
        });

        braintree.finishPayWithVenmo(Activity.RESULT_OK, new Intent());
        SystemClock.sleep(50);
        assertTrue(nonceListenerCalled.get());
        assertTrue(paymentMethodListenerCalled.get());
    }

    public void testFinishPayWithVenmoDoesNothingOnNullBuilder() throws ConfigurationException {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.finishPayWithVenmo(eq(Activity.RESULT_CANCELED), eq(intent))).
                thenReturn(null);

        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean listenerWasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onUnrecoverableError(Throwable throwable) {
                listenerWasCalled.set(true);
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                listenerWasCalled.set(true);
            }
        });

        braintree.finishPayWithVenmo(Activity.RESULT_CANCELED, intent);
        SystemClock.sleep(50);

        assertFalse("Expected no listeners to fire but one did fire", listenerWasCalled.get());
    }
    public void testRemovedListenerIsNotPostedTo() throws ExecutionException, InterruptedException {
        createCardSync(mBraintree);

        final AtomicBoolean wasRemoved = new AtomicBoolean(false);
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                wasCalled.set(true);
                if (wasRemoved.get()) {
                    fail("Listener was not removed correctly.");
                }
            }
        };

        mBraintree.addListener(listener);

        mBraintree.getPaymentMethodsHelper().get();
        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        mBraintree.removeListener(listener);
        wasRemoved.set(true);

        mBraintree.getPaymentMethodsHelper().get();
        TestUtils.waitForMainThreadToFinish();
    }

    public void testDoesNotExecuteCallbackWithNoListeners()
            throws ExecutionException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        ListenerCallback listenerCallback = new ListenerCallback() {
            @Override
            public void execute() {
                wasCalled.set(true);
            }

            @Override
            public boolean hasListeners() {
                return false;
            }
        };

        mBraintree.postOrQueueCallback(listenerCallback);
        TestUtils.waitForMainThreadToFinish();
        assertFalse(wasCalled.get());
    }

    public void testEnqueuesCallbacksWithNoListeners() throws InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        final AtomicBoolean hasListener = new AtomicBoolean(false);
        ListenerCallback listenerCallback = new ListenerCallback() {
            @Override
            public void execute() {
                wasCalled.set(true);
            }

            @Override
            public boolean hasListeners() {
                return hasListener.get();
            }
        };

        mBraintree.postOrQueueCallback(listenerCallback);
        TestUtils.waitForMainThreadToFinish();
        assertFalse(wasCalled.get());

        hasListener.set(true);
        mBraintree.unlockListeners();
        TestUtils.waitForMainThreadToFinish();

        assertTrue("Expected callback to have been called", wasCalled.get());
    }

    public void testRemovedErrorListenerIsNotPostedTo()
            throws ExecutionException, InterruptedException, UnexpectedException {
        BraintreeApi braintreeApi = TestUtils.unexpectedExceptionThrowingApi(getContext(),
                TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json"));
        Braintree braintree = new Braintree(braintreeApi);

        final AtomicBoolean wasRemoved = new AtomicBoolean(false);
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                wasCalled.set(true);
                if (wasRemoved.get()) {
                    fail("Listener was not removed correctly.");
                }
            }
        };

        braintree.addListener(listener);

        braintree.getPaymentMethodsHelper().get();
        TestUtils.waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        braintree.removeListener(listener);
        wasRemoved.set(true);

        braintree.getPaymentMethodsHelper().get();
        TestUtils.waitForMainThreadToFinish();
    }

    public void testSameBraintreeIsRetrievedForIdenticalClientTokens() {
        String clientToken = new TestClientTokenBuilder().build();

        Braintree b1 = Braintree.getInstance(getContext(), clientToken);
        Braintree b2 = Braintree.getInstance(getContext(), clientToken);

        assertTrue(b1 == b2);
    }

    public void testCanAddMultipleListeners() throws ExecutionException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = Braintree.getInstance(getContext(), clientToken);

        final AtomicBoolean wasFirstListenerCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                wasFirstListenerCalled.set(true);
            }
        });

        final AtomicBoolean wasSecondListenerCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                wasSecondListenerCalled.set(true);
            }
        });

        createCardSync(braintree);
        TestUtils.waitForMainThreadToFinish();

        assertTrue(wasFirstListenerCalled.get());
        assertTrue(wasSecondListenerCalled.get());
    }

    public void testCanLockAndUnlockListeners() throws ExecutionException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = Braintree.getInstance(getContext(), clientToken);

        final AtomicInteger callCount = new AtomicInteger(0);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                callCount.incrementAndGet();
            }
        });

        braintree.lockListeners();

        createCardSync(braintree);
        TestUtils.waitForMainThreadToFinish();
        assertEquals(0, callCount.get());

        braintree.unlockListeners();
        TestUtils.waitForMainThreadToFinish();
        assertEquals(1, callCount.get());

        braintree.unlockListeners();
        TestUtils.waitForMainThreadToFinish();
        assertEquals("Queued listeners were executed multiple times", 1, callCount.get());
    }

    public void testCanSwitchOutListenerDuringLockedPhaseAndOnlySecondListenerIsExecuted()
            throws ExecutionException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        Braintree braintree = Braintree.getInstance(getContext(), clientToken);

        class TrackingListener extends SimpleListener {
            AtomicBoolean mWasCalled = new AtomicBoolean(false);

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                mWasCalled.set(true);
            }

            public boolean wasCalled() {
                return mWasCalled.get();
            }
        }

        TrackingListener firstListener = new TrackingListener();
        TrackingListener secondListener = new TrackingListener();

        braintree.addListener(firstListener);
        braintree.lockListeners();
        createCardSync(braintree);
        TestUtils.waitForMainThreadToFinish();

        braintree.removeListener(firstListener);
        braintree.addListener(secondListener);
        braintree.unlockListeners();
        TestUtils.waitForMainThreadToFinish();

        assertFalse(firstListener.wasCalled());
        assertTrue(secondListener.wasCalled());
    }

    public void testProxiesSendAnalyticsEventToBraintreeApi()
            throws ExecutionException, InterruptedException {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        Braintree braintree = new Braintree(braintreeApi);

        verify(braintreeApi, never()).sendAnalyticsEvent(anyString(), anyString());
        braintree.sendAnalyticsEventHelper("event", "TEST").get();
        verify(braintreeApi, times(1)).sendAnalyticsEvent("event", "TEST");
    }

    /** helper to synchronously create a credit card */
    private static void createCardSync(Braintree braintree) throws ExecutionException,
            InterruptedException {
        braintree.createHelper(new CardBuilder()
                .cardNumber("4111111111111111")
                .expirationMonth("04")
                .expirationYear("17")).get();
    }

    /**
     * Simple listener that allows implementers to only override the methods they need.
     */
    private static abstract class SimpleListener implements PaymentMethodsUpdatedListener,
            PaymentMethodCreatedListener, PaymentMethodNonceListener, ErrorListener {
        @Override
        public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {}

        @Override
        public void onPaymentMethodCreated(PaymentMethod paymentMethod) {}

        @Override
        public void onPaymentMethodNonce(String paymentMethodNonce) {}

        @Override
        public void onUnrecoverableError(Throwable throwable) {
            throw new RuntimeException("An UnrecoverableError occurred: " + throwable.getClass() +
                    ": " + throwable.getMessage());
        }

        @Override
        public void onRecoverableError(ErrorWithResponse error) {
            throw new RuntimeException("A RecoverableError occurred: " + error.getMessage());
        }
    }
}
