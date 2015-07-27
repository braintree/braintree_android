package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.Braintree.ListenerCallback;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.test.AbstractBraintreeListener;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.braintreepayments.test.TestListenerActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.create;
import static com.braintreepayments.api.BraintreeTestUtils.getBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.tokenize;
import static com.braintreepayments.api.BraintreeTestUtils.waitForMainThreadToFinish;
import static com.braintreepayments.api.internal.BraintreeHttpClientTestUtils.waitForHttpClientToIdle;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BraintreeTest {

    private String mClientToken;
    private Braintree mBraintree;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws InterruptedException {
        BraintreeTestUtils.setUp(getTargetContext());
        mClientToken = new TestClientTokenBuilder().build();
        mBraintree = getBraintree(getTargetContext(), mClientToken);
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void setup_isSuccessful() throws InterruptedException {
        Braintree.setup(getTargetContext(), mClientToken, new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertTrue(setupSuccessful);
                assertNotNull(braintree);
                assertNull(errorMessage);
                assertNull(exception);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void setup_isSuccessfulWhenCalledMultipleTimes() throws InterruptedException {
        BraintreeSetupFinishedListener setupFinishedListener = new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertTrue(setupSuccessful);
                assertNotNull(braintree);
                assertNull(errorMessage);
                assertNull(exception);
                mCountDownLatch.countDown();
            }
        };

        Braintree.setup(getTargetContext(), mClientToken, setupFinishedListener);
        mCountDownLatch.await();
        mCountDownLatch = new CountDownLatch(1);

        Braintree.setup(getTargetContext(), mClientToken, setupFinishedListener);
        mCountDownLatch.await();
        mCountDownLatch = new CountDownLatch(1);

        Braintree.setup(getTargetContext(), mClientToken, setupFinishedListener);
        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void setup_returnsAnError() throws JSONException, InterruptedException {
        Braintree braintree = new Braintree(getTargetContext(), mClientToken);
        braintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new UnexpectedException("Error"));
            }
        };

        Braintree.setup(getTargetContext(), mClientToken, new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertFalse(setupSuccessful);
                assertNull(braintree);
                assertEquals("Error", errorMessage);
                assertTrue(exception instanceof UnexpectedException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setup_returnsAnErrorForNonJsonClientToken() throws InterruptedException {
        Braintree.setup(getTargetContext(), "not-json!", new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertFalse(setupSuccessful);
                assertNull(braintree);
                assertEquals(
                        "Value not-json! of type java.lang.String cannot be converted to JSONObject",
                        errorMessage);
                assertTrue(exception instanceof JSONException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void create_createsACard() throws InterruptedException {
        create(mBraintree, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        assertEquals(1, mBraintree.getCachedPaymentMethods().size());
        assertTrue(mBraintree.getCachedPaymentMethods().get(0) instanceof Card);
        assertEquals("11", ((Card) mBraintree.getCachedPaymentMethods().get(0)).getLastTwo());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void create_postsCardsToListeners() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(2);
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }

            @Override
            public void onPaymentMethodNonce(String nonce) {
                assertNotNull(nonce);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void tokenize_postsNonceToListeners() throws InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.tokenize(new CardBuilder().cardNumber("55"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void getPaymentMethods_postsCardsToListeners() throws InterruptedException {
        create(mBraintree, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                fail("onPaymentMethodCreated should not be called for tokenizing");
            }

            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                assertTrue(paymentMethods.size() > 0);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.getPaymentMethods();

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void onUnrecoverableErrorsPostsToListeners() throws InterruptedException {
        mBraintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.failure(new UnexpectedException("Error"));
            }
        };
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable instanceof UnexpectedException);
                assertEquals("Error", throwable.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mBraintree.create(new CardBuilder());

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void onRecoverableErrorsPostsToListeners() throws InterruptedException {
        mBraintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void post(String path, String data, HttpResponseCallback callback) {
                callback.failure(new ErrorWithResponse(422,
                        stringFromFixture(getTargetContext(), "error_response.json")));
            }
        };
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals("There was an error", error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mBraintree.create(new CardBuilder());

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendAnalyticsEvent_sendsAnalyticsIfEnabled() throws JSONException {
        String configuration = stringFromFixture(getTargetContext(), "configuration_with_analytics.json");
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, configuration);
        braintree.mHttpClient = mock(BraintreeHttpClient.class);

        braintree.sendAnalyticsEvent("very.important.analytics-payload");

        verify(braintree.mHttpClient, times(1)).post(matches("analytics_url"),
                contains("very.important.analytics-payload"), any(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendAnalyticsEvent_noopsIfDisabled() throws JSONException {
        String configuration = stringFromFixture(getTargetContext(),
                "configuration_without_analytics.json");
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, configuration);
        braintree.mHttpClient = mock(BraintreeHttpClient.class);

        braintree.sendAnalyticsEvent("event");

        verify(braintree.mHttpClient, never()).post(anyString(), anyString(),
                any(HttpResponseCallback.class));
    }

    @Test(timeout = 10000)
    @MediumTest
    public void sendAnalyticsEvent_sendsEventsToServer() throws JSONException,
            InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger(0);
        ClientToken clientToken = ClientToken.fromString(mClientToken);
        BraintreeHttpClient httpClient = new BraintreeHttpClient(clientToken.getAuthorizationFingerprint()) {
            @Override
            public void post(String url, String params, final HttpResponseCallback callback) {
                super.post(url, params, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        requestCount.incrementAndGet();
                    }

                    @Override
                    public void failure(Exception exception) {
                        throw new RuntimeException("Request failure: " + exception.getMessage());
                    }
                });
            }
        };
        Configuration configuration = Configuration.fromJson(mClientToken);
        httpClient.setBaseUrl(configuration.getClientApiUrl());
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, mClientToken);
        braintree.mHttpClient = httpClient;

        braintree.sendAnalyticsEvent("event");
        waitForHttpClientToIdle(braintree.mHttpClient);
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();
        assertEquals(1, requestCount.get());

        braintree.sendAnalyticsEvent("another-event");
        waitForHttpClientToIdle(braintree.mHttpClient);
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();
        assertEquals(2, requestCount.get());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithPayPal_postConfigurationExceptionForBadResultExtras()
            throws InterruptedException {
        mBraintree = getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable error) {
                assertTrue(error instanceof ConfigurationException);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                new Intent());

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startPayWithPayPal_startsPayPal() throws InterruptedException {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        Braintree braintree = spy(getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build()));
        Activity mockActivity = mock(Activity.class);

        braintree.startPayWithPayPal(mockActivity, 0);

        verify(mockActivity).startActivityForResult(launchIntentCaptor.capture(), eq(0));

        Intent intent = launchIntentCaptor.getValue();
        assertEquals(PayPalProfileSharingActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startPayWithPayPal_sendsAnalyticsEvent() throws JSONException,
            InterruptedException {
        Braintree braintree = spy(getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build()));

        braintree.startPayWithPayPal(mock(Activity.class), 0);

        verify(braintree).sendAnalyticsEvent("add-paypal.start");
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithPayPal_callsNoListenersOnNullBuilder() throws InterruptedException {
        mBraintree = getBraintree(getTargetContext(),
                new TestClientTokenBuilder().withPayPal().build());
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                fail("Listener was called");
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                fail("Listener was called");
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                fail("Listener was called");
            }

            @Override
            public void onUnrecoverableError(Throwable throwable) {
                fail("Listener was called");
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                fail("Listener was called");
            }
        });

        mBraintree.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_CANCELED,
                new Intent());

        SystemClock.sleep(100);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startPayWithVenmo_appSwitchesWithVenmoLaunchIntent() {
        ArgumentCaptor<Intent> launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);
        mBraintree = spy(mBraintree);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);
        Activity mockActivity = mock(Activity.class);

        mBraintree.startPayWithVenmo(mockActivity, 0);

        verify(mockActivity).startActivityForResult(launchIntentCaptor.capture(), eq(0));
        Intent launchIntent = launchIntentCaptor.getValue();
        assertEquals("com.venmo/com.venmo.CardChooserActivity",
                launchIntent.getComponent().flattenToString());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startPayWithVenmo_sendsAnalyticsEvent() {
        mBraintree = spy(mBraintree);
        when(mBraintree.isVenmoEnabled()).thenReturn(true);

        mBraintree.startPayWithVenmo(mock(Activity.class), 0);

        verify(mBraintree).sendAnalyticsEvent("add-venmo.start");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void startPayWithVenmo_sendsAnalyticsEventWhenUnavailable() {
        mBraintree = spy(mBraintree);
        when(mBraintree.isVenmoEnabled()).thenReturn(false);

        mBraintree.startPayWithVenmo(null, 0);

        InOrder order = inOrder(mBraintree);
        order.verify(mBraintree).sendAnalyticsEvent("add-venmo.start");
        order.verify(mBraintree).sendAnalyticsEvent("add-venmo.unavailable");
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithVenmo_postsNonceAndPaymentMethodOnSuccess()
            throws JSONException, InterruptedException {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");
        mBraintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.success(stringFromFixture(getTargetContext(),
                        "payment_methods/get_payment_method_card_response.json"));
            }
        };

        mCountDownLatch = new CountDownLatch(2);
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod method) {
                assertEquals("123456-12345-12345-a-adfa", method.getNonce());
                assertEquals("11", ((Card) method).getLastTwo());
                mCountDownLatch.countDown();
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertEquals("123456-12345-12345-a-adfa", paymentMethodNonce);
                mCountDownLatch.countDown();
            }
        });

        mBraintree.finishPayWithVenmo(Activity.RESULT_OK, intent);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithVenmo_postsExceptionToListener()
            throws JSONException, InterruptedException {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");
        mBraintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new Exception());
            }
        };
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                mCountDownLatch.countDown();
            }
        });

        mBraintree.finishPayWithVenmo(Activity.RESULT_OK, intent);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithVenmo_sendsAnalyticsEventOnSuccess()
            throws JSONException, InterruptedException {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, mClientToken) {
            @Override
            public synchronized void sendAnalyticsEvent(String eventFragment) {
                assertEquals("venmo-app.success", eventFragment);
                mCountDownLatch.countDown();
            }
        };
        braintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.success(stringFromFixture(getTargetContext(),
                        "payment_methods/get_payment_method_card_response.json"));
            }
        };

        braintree.finishPayWithVenmo(Activity.RESULT_OK, intent);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithVenmo_sendsAnalyticsEventMissingNonce()
            throws JSONException, InterruptedException {
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, mClientToken) {
            @Override
            public synchronized void sendAnalyticsEvent(String eventFragment) {
                assertEquals("venmo-app.fail", eventFragment);
                mCountDownLatch.countDown();
            }
        };

        braintree.finishPayWithVenmo(Activity.RESULT_OK, new Intent());

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void finishPayWithVenmo_sendsAnalyticsEventOnFailure()
            throws JSONException, InterruptedException {
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE,
                "123456-12345-12345-a-adfa");
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, mClientToken) {
            @Override
            public synchronized void sendAnalyticsEvent(String eventFragment) {
                assertEquals("venmo-app.fail", eventFragment);
                mCountDownLatch.countDown();
            }
        };
        braintree.mHttpClient = new BraintreeHttpClient("") {
            @Override
            public void get(String path, HttpResponseCallback callback) {
                callback.failure(new Exception());
            }
        };

        braintree.finishPayWithVenmo(Activity.RESULT_OK, intent);

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startThreeDSecureVerification_postsPaymentMethodToListenersWhenLookupReturnsACard()
            throws JSONException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        Braintree braintree = new Braintree(getTargetContext(), clientToken, clientToken);

        mCountDownLatch = new CountDownLatch(2);
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                mCountDownLatch.countDown();
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("51", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        String nonce = tokenize(braintree, new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20"));

        braintree.startThreeDSecureVerification(null, 0, nonce, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void startThreeDSecureVerification_acceptsACardBuilderAndPostsAPaymentMethodToListener()
            throws JSONException, InterruptedException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        Braintree braintree = new Braintree(getTargetContext(), clientToken, clientToken);

        mCountDownLatch = new CountDownLatch(2);
        braintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                mCountDownLatch.countDown();
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("51", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");
        braintree.startThreeDSecureVerification(null, 0, cardBuilder, "5");

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finishThreeDSecureVerification_postsPaymentMethodToListener()
            throws JSONException, InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                mCountDownLatch.countDown();
            }
        });

        JSONObject authResponse = new JSONObject(stringFromFixture(getTargetContext(),
                "three_d_secure/authentication_response.json"));

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finishThreeDSecureVerification_postsNonceToListener()
            throws JSONException, InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertEquals("123456-12345-12345-a-adfa", paymentMethodNonce);
                mCountDownLatch.countDown();
            }
        });

        JSONObject authResponse = new JSONObject(stringFromFixture(getTargetContext(),
                "three_d_secure/authentication_response.json"));

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finishThreeDSecureVerification_postsUnrecoverableErrorsToListeners()
            throws InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertEquals("Error!", throwable.getMessage());
                mCountDownLatch.countDown();
            }
        });

        ThreeDSecureAuthenticationResponse authResponse =
                ThreeDSecureAuthenticationResponse.fromException("Error!");

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, authResponse);

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finishThreeDSecureVerification_postsRecoverableErrorsToListener()
            throws JSONException, InterruptedException {
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals("Failed to authenticate, please try a different form of payment",
                        error.getMessage());
                mCountDownLatch.countDown();
            }
        });

        String response = stringFromFixture(getTargetContext(), "errors/three_d_secure_error.json");

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(response));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finishThreeDSecureVerification_doesNothingWhenResultCodeNotOk() {
        Intent intent = mock(Intent.class);

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_CANCELED, intent);

        verifyZeroInteractions(intent);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getAndroidPayPaymentMethodTokenizationParameters_returnsCorrectParameters()
            throws JSONException {
        String configuration = stringFromFixture(getTargetContext(), "configuration_with_android_pay.json");
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, configuration);

        Bundle tokenizationParameters =
                braintree.getAndroidPayTokenizationParameters().getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals("integration_merchant_id",
                tokenizationParameters.getString("braintree:merchantId"));
        assertEquals("google-auth-fingerprint",
                tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME,
                tokenizationParameters.getString("braintree:sdkVersion"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void doesNotExecuteCallbackWithNoListeners() throws InterruptedException {
        ListenerCallback listenerCallback = new ListenerCallback() {
            @Override
            public void execute() {
                mCountDownLatch.countDown();
            }

            @Override
            public boolean hasListeners() {
                return false;
            }
        };

        mBraintree.postOrQueueCallback(listenerCallback);
        waitForMainThreadToFinish();
        assertEquals(1, mCountDownLatch.getCount());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void enqueuesCallbacksWithNoListeners() throws InterruptedException {
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
        waitForMainThreadToFinish();
        assertFalse(wasCalled.get());

        hasListener.set(true);
        mBraintree.unlockListeners();
        waitForMainThreadToFinish();

        assertTrue("Expected callback to have been called", wasCalled.get());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void removedErrorListenerIsNotPostedTo() throws JSONException, InterruptedException {
        Braintree braintree = new Braintree(getTargetContext(), mClientToken) {
            @Override
            public synchronized void getPaymentMethods() {
                postExceptionToListeners(new Exception());
            }
        };

        final AtomicBoolean wasRemoved = new AtomicBoolean(false);
        AbstractBraintreeListener listener = new AbstractBraintreeListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                mCountDownLatch.countDown();
                if (wasRemoved.get()) {
                    fail("Listener was not removed correctly.");
                }
            }
        };

        braintree.addListener(listener);

        braintree.getPaymentMethods();
        mCountDownLatch.await();

        braintree.removeListener(listener);
        wasRemoved.set(true);

        braintree.getPaymentMethods();
        waitForMainThreadToFinish();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sameBraintreeIsRetrievedForIdenticalClientTokens()
            throws JSONException, InterruptedException {
        Braintree b1 = getBraintree(getTargetContext(), mClientToken);
        Braintree b2 = getBraintree(getTargetContext(), mClientToken);

        assertTrue(b1 == b2);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canAddMultipleListeners()
            throws ExecutionException, InterruptedException, JSONException {
        mCountDownLatch = new CountDownLatch(2);
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                mCountDownLatch.countDown();
            }
        });

        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                mCountDownLatch.countDown();
            }
        });

        create(mBraintree, new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canLockAndUnlockListeners() throws InterruptedException {
        final AtomicInteger callCount = new AtomicInteger(0);
        mBraintree.addListener(new AbstractBraintreeListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                callCount.incrementAndGet();
            }
        });

        mBraintree.lockListeners();

        mBraintree.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));
        waitForHttpClientToIdle(mBraintree.mHttpClient);
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();
        assertEquals(0, callCount.get());

        mBraintree.unlockListeners();
        waitForMainThreadToFinish();
        assertEquals(1, callCount.get());

        mBraintree.unlockListeners();
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();
        assertEquals("Queued listeners were executed multiple times", 1, callCount.get());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void canSwitchOutListenerDuringLockedPhaseAndOnlySecondListenerIsExecuted()
            throws InterruptedException {
        class TrackingListener extends AbstractBraintreeListener {
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

        mBraintree.addListener(firstListener);
        mBraintree.lockListeners();
        mBraintree.create(new CardBuilder()
                .cardNumber(VISA)
                .expirationMonth("04")
                .expirationYear("17"));
        waitForHttpClientToIdle(mBraintree.mHttpClient);
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();

        mBraintree.removeListener(firstListener);
        mBraintree.addListener(secondListener);
        mBraintree.unlockListeners();
        SystemClock.sleep(1000);
        waitForMainThreadToFinish();

        assertFalse(firstListener.wasCalled());
        assertTrue(secondListener.wasCalled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_handlesPayPalResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishPayWithPayPal(any(Activity.class), anyInt(),
                any(Intent.class));
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION, "");

        braintree.onActivityResult(null, 0, responseCode, intent);

        verify(braintree).finishPayWithPayPal(null, responseCode, intent);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_handlesAndroidPayMaskedWalletResults() {
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_MASKED_WALLET, "");
        Braintree braintree = spy(mBraintree);
        doReturn(null).when(braintree).getAndroidPayGoogleTransactionId(intent);
        doNothing().when(braintree).performAndroidPayFullWalletRequest(any(Activity.class),
                anyInt(), any(Cart.class), anyString());
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).performAndroidPayFullWalletRequest(null, requestCode, null, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_handlesAndroidPayFullWalletResults() {
        Braintree braintree = spy(mBraintree);
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_FULL_WALLET, "");
        doNothing().when(braintree).getNonceFromAndroidPayFullWalletResponse(Activity.RESULT_OK,
                intent);
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).getNonceFromAndroidPayFullWalletResponse(responseCode, intent);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_handlesVenmoResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishPayWithVenmo(anyInt(), any(Intent.class));
        Intent intent = new Intent().putExtra(Venmo.EXTRA_PAYMENT_METHOD_NONCE, "");
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).finishPayWithVenmo(responseCode, intent);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_handlesThreeDSecureResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishThreeDSecureVerification(anyInt(), any(Intent.class));
        Intent intent = new Intent().putExtra(
                ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, "");
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).finishThreeDSecureVerification(responseCode, intent);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onActivityResult_doesNothingForUnknownActivity() {
        Braintree braintree = spy(mBraintree);
        Intent intent = new Intent();
        int requestCode = 10;
        int resultCode = Activity.RESULT_OK;

        braintree.onActivityResult(null, requestCode, resultCode, intent);

        verify(braintree).onActivityResult(null, requestCode, resultCode, intent);
        verifyNoMoreInteractions(braintree);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onResumeAdds_listenersAndUnlocksListeners() {
        Braintree braintree = spy(mBraintree);
        Activity listenerActivity = mock(TestListenerActivity.class);

        braintree.onResume(listenerActivity);

        verify(braintree).addListener((PaymentMethodNonceListener) listenerActivity);
        verify(braintree).unlockListeners();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onPauseLocks_listenersAndRemovesListeners() {
        Braintree braintree = spy(mBraintree);
        Activity listenerActivity = mock(TestListenerActivity.class);

        braintree.onPause(listenerActivity);

        verify(braintree).lockListeners();
        verify(braintree).removeListener((PaymentMethodNonceListener) listenerActivity);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void onSaveInstanceState_addsClientTokenAndConfigurationToBundle()
            throws JSONException {
        Braintree braintree = new Braintree(getTargetContext(), mClientToken, mClientToken);
        Bundle bundle = new Bundle();

        braintree.onSaveInstanceState(bundle);

        assertEquals(mClientToken, bundle.getString("com.braintreepayments.api.KEY_CLIENT_TOKEN"));
        assertEquals(mClientToken, bundle.getString("com.braintreepayments.api.KEY_CONFIGURATION"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void restoreSavedInstanceState_returnsNullIfBundleIsNull() {
        assertNull(Braintree.restoreSavedInstanceState(getTargetContext(), null));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void restoreSavedInstanceState_returnsBraintreeInstanceIfThereIsAnExistingInstance() {
        Bundle bundle = new Bundle();

        mBraintree.onSaveInstanceState(bundle);
        Braintree restoredBraintree = Braintree.restoreSavedInstanceState(getTargetContext(), bundle);

        assertEquals(mBraintree, restoredBraintree);
    }

    @Test(timeout = 10000)
    @MediumTest
    public void restoreSavedInstanceState_returnsANewBraintreeInstanceIfThereIsNoExistingInstance() {
        Braintree.reset();
        Bundle bundle = new Bundle();
        bundle.putString("com.braintreepayments.api.KEY_CLIENT_TOKEN", mClientToken);
        bundle.putString("com.braintreepayments.api.KEY_CONFIGURATION", mClientToken);

        assertTrue(Braintree.sInstances.size() == 0);
        Braintree braintree = Braintree.restoreSavedInstanceState(getTargetContext(), bundle);
        assertTrue(braintree != null);
        assertTrue(Braintree.sInstances.size() == 1);
    }
}
