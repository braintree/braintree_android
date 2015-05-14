package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.ListenerCallback;
import com.braintreepayments.api.Braintree.PaymentMethodCreatedListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.Braintree.PaymentMethodsUpdatedListener;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;
import com.braintreepayments.api.threedsecure.ThreeDSecureWebViewActivity;
import com.braintreepayments.test.TestListenerActivity;
import com.braintreepayments.testutils.TestClientTokenBuilder;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.WalletConstants;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.api.TestUtils.apiWithExpectedResponse;
import static com.braintreepayments.api.TestUtils.unexpectedExceptionThrowingApi;
import static com.braintreepayments.api.TestUtils.waitForMainThreadToFinish;
import static com.braintreepayments.testutils.CardNumber.VISA;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BraintreeTest extends AndroidTestCase {

    private static final String TEST_CLIENT_TOKEN_KEY = "TEST_CLIENT_TOKEN_KEY";

    Braintree mBraintree;
    BraintreeApi mBraintreeApi;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.setUp(getContext());
        String clientToken = new TestClientTokenBuilder().build();
        mBraintreeApi = new BraintreeApi(getContext(), clientToken);
        mBraintree = new Braintree(clientToken, mBraintreeApi);
    }

    public void testSetupIsSuccessful() throws ExecutionException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        BraintreeSetupFinishedListener setupFinishedListener = new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertTrue(setupSuccessful);
                assertNotNull(braintree);
                assertNull(errorMessage);
                assertNull(exception);
                wasCalled.set(true);
            }
        };

        Braintree.setupHelper(mContext, new TestClientTokenBuilder().build(), setupFinishedListener)
                .get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testSetupReturnsAnError()
            throws ExecutionException, InterruptedException, ErrorWithResponse, BraintreeException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        BraintreeSetupFinishedListener setupFinishedListener = new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful, Braintree braintree,
                    String errorMessage, Exception exception) {
                assertFalse(setupSuccessful);
                assertNull(braintree);
                assertEquals("Error!", errorMessage);
                assertTrue(exception instanceof AuthenticationException);
                wasCalled.set(true);
            }
        };

        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        doThrow(new AuthenticationException("Error!")).when(braintreeApi).setup();
        Braintree braintree = new Braintree(clientToken, braintreeApi);
        Braintree.sInstances.put(clientToken, braintree);
        Braintree.setupHelper(mContext, clientToken, setupFinishedListener).get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
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

        waitForMainThreadToFinish();
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

        waitForMainThreadToFinish();
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

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testOnUnrecoverableErrorsPostsToListeners()
            throws ExecutionException, InterruptedException, BraintreeException, ErrorWithResponse {
        BraintreeApi braintreeApi = unexpectedExceptionThrowingApi(getContext());
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertTrue(throwable != null);
                wasCalled.set(true);
            }
        });

        braintree.getPaymentMethodsHelper().get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.createHelper(new CardBuilder()).get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testOnRecoverableErrorsPostsToListeners()
            throws ExecutionException, InterruptedException, IOException, ErrorWithResponse {
        String response = stringFromFixture(getContext(), "error_response.json");
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY,
                apiWithExpectedResponse(getContext(), 422, response));

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertTrue(error != null);
                wasCalled.set(true);
            }
        });

        braintree.getPaymentMethodsHelper().get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.createHelper(new CardBuilder()).get();

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testPayPalConfigurationExceptionPostsToOnUnrecoverableError()
            throws BraintreeException, InterruptedException, ErrorWithResponse {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.handlePayPalResponse(any(Activity.class), eq(PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID), eq(intent)))
                .thenThrow(new ConfigurationException("Test"));

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable error) {
                assertTrue(error != null);
                wasCalled.set(true);
            }
        });

        braintree.finishPayWithPayPal(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                intent);
        SystemClock.sleep(50);
        assertTrue(wasCalled.get());

        wasCalled.set(false);
        braintree.handlePayPalResponse(null, PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID,
                intent);
        SystemClock.sleep(50);
        assertTrue(wasCalled.get());
    }

    public void testStartPayWithPayPalSendsAnalyticsEvent() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        braintree.startPayWithPayPal(null, 1);
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("custom.android.add-paypal.start", "custom");
    }

    public void testFinishPayWithPayPalDoesNothingOnNullBuilder() throws ConfigurationException {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.handlePayPalResponse(any(Activity.class), eq(PayPalFuturePaymentActivity.RESULT_CANCELED), eq(intent))).
                thenReturn(null);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

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

    public void testStartPayWithVenmoSendsAnalyticsEvent() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        braintree.startPayWithVenmo(null, 1);
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("custom.android.add-venmo.start", "custom");
    }

    public void testStartPayWithVenmoSendsAnalyticsEventForDropin() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        braintree.setIntegrationDropin();
        braintree.startPayWithVenmo(null, 1);
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("dropin.android.add-venmo.start", "dropin");
    }

    public void testStartPayWithVenmoSendsAnalyticsEventWhenUnavailable()
            throws AppSwitchNotAvailableException {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        doThrow(new AppSwitchNotAvailableException()).when(braintreeApi).startPayWithVenmo(
                any(Activity.class), anyInt());

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        braintree.startPayWithVenmo(null, 1);
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("custom.android.add-venmo.unavailable", "custom");
    }

    public void testFinishPayWithVenmoPostsNonceAndPaymentMethodOnSuccess()
            throws JSONException, BraintreeException, ErrorWithResponse {
        final PaymentMethod paymentMethod = new PaymentMethod() {
            @Override
            public String getTypeLabel() {
                return "I am a payment method";
            }

            @Override
            public int describeContents() { return 0; }

            @Override
            public void writeToParcel(Parcel dest, int flags) {}
        };
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.finishPayWithVenmo(eq(Activity.RESULT_OK), any(Intent.class))).thenReturn(
                "nonce");
        when(braintreeApi.getPaymentMethod("nonce")).thenReturn(paymentMethod);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        final AtomicBoolean nonceListenerCalled = new AtomicBoolean(false);
        final AtomicBoolean paymentMethodListenerCalled = new AtomicBoolean(false);
        braintree.addListener(new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod method) {
                assertEquals(paymentMethod, method);
                paymentMethodListenerCalled.set(true);
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertEquals("nonce", paymentMethodNonce);
                nonceListenerCalled.set(true);
            }
        });

        braintree.finishPayWithVenmo(Activity.RESULT_OK, new Intent());
        SystemClock.sleep(50);
        assertTrue(nonceListenerCalled.get());
        assertTrue(paymentMethodListenerCalled.get());
    }

    public void testFinishPayWithVenmoSendsAnalyticsEventOnSuccess()
            throws JSONException, BraintreeException, ErrorWithResponse {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);
        when(braintreeApi.finishPayWithVenmo(eq(Activity.RESULT_OK), any(Intent.class))).thenReturn(
                "nonce");
        when(braintreeApi.getPaymentMethod("nonce")).thenReturn(new Card());

        braintree.finishPayWithVenmo(Activity.RESULT_OK, new Intent());
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("custom.android.venmo-app.success", "custom");
    }

    public void testFinishPayWithVenmoSendsAnalyticsEventOnFailure() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

        braintree.finishPayWithVenmo(Activity.RESULT_OK, new Intent());
        SystemClock.sleep(50);
        verify(braintreeApi).sendAnalyticsEvent("custom.android.venmo-app.fail", "custom");
    }

    public void testFinishPayWithVenmoDoesNothingOnNullBuilder() throws ConfigurationException {
        Intent intent = new Intent();
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.finishPayWithVenmo(eq(Activity.RESULT_CANCELED), eq(intent))).
                thenReturn(null);

        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

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

    public void testStartThreeDSecureVerificationPostsPaymentMethodToListenersWhenLookupReturnsACard()
            throws InterruptedException, ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeApi braintreeApi = new BraintreeApi(getContext(), clientToken);
        Braintree braintree = new Braintree(clientToken, braintreeApi);

        final AtomicBoolean paymentMethodNonceCalled = new AtomicBoolean(false);
        final AtomicBoolean paymentMethodCreatedCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                paymentMethodNonceCalled.set(true);
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("51", ((Card) paymentMethod).getLastTwo());
                paymentMethodCreatedCalled.set(true);
            }
        };
        braintree.addListener(listener);

        String nonce = braintreeApi.tokenize(new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20"));

        braintree.startThreeDSecureVerification(null, 0, nonce, "5");

        SystemClock.sleep(5000);

        assertTrue(paymentMethodNonceCalled.get());
        assertTrue(paymentMethodCreatedCalled.get());
    }

    public void testStartThreeDSecureVerificationAcceptsACardBuilderAndPostsAPaymentMethodToListener() {
        String clientToken = new TestClientTokenBuilder().withThreeDSecure().build();
        BraintreeApi braintreeApi = new BraintreeApi(getContext(), clientToken);
        Braintree braintree = new Braintree(clientToken, braintreeApi);

        final AtomicBoolean paymentMethodNonceCalled = new AtomicBoolean(false);
        final AtomicBoolean paymentMethodCreatedCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertNotNull(paymentMethodNonce);
                paymentMethodNonceCalled.set(true);
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("51", ((Card) paymentMethod).getLastTwo());
                paymentMethodCreatedCalled.set(true);
            }
        };
        braintree.addListener(listener);

        CardBuilder cardBuilder = new CardBuilder()
                .cardNumber("4000000000000051")
                .expirationDate("12/20");
        braintree.startThreeDSecureVerification(null, 0, cardBuilder, "5");

        SystemClock.sleep(5000);

        assertTrue(paymentMethodNonceCalled.get());
        assertTrue(paymentMethodCreatedCalled.get());
    }

    public void testFinishThreeDSecureVerificationPostsPaymentMethodToListener()
            throws JSONException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                assertEquals("11", ((Card) paymentMethod).getLastTwo());
                wasCalled.set(true);
            }
        };
        mBraintree.addListener(listener);

        JSONObject authResponse = new JSONObject(stringFromFixture(mContext,
                "three_d_secure/authentication_response.json"));

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testFinishThreeDSecureVerificationPostsNonceToListener()
            throws JSONException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                assertEquals("123456-12345-12345-a-adfa", paymentMethodNonce);
                wasCalled.set(true);
            }
        };
        mBraintree.addListener(listener);

        JSONObject authResponse = new JSONObject(stringFromFixture(mContext,
                "three_d_secure/authentication_response.json"));

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(authResponse.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testFinishThreeDSecureVerificationPostsUnrecoverableErrorsToListeners()
            throws InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onUnrecoverableError(Throwable throwable) {
                assertEquals("Error!", throwable.getMessage());
                wasCalled.set(true);
            }
        };
        mBraintree.addListener(listener);

        ThreeDSecureAuthenticationResponse authResponse =
                ThreeDSecureAuthenticationResponse.fromException("Error!");

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, authResponse);

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testFinishThreeDSecureVerificationPostsRecoverableErrorsToListener()
            throws InterruptedException, JSONException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                assertEquals("Failed to authenticate, please try a different form of payment", error.getMessage());
                wasCalled.set(true);
            }
        };
        mBraintree.addListener(listener);

        JSONObject response =
                new JSONObject(stringFromFixture(mContext, "errors/three_d_secure_error.json"));

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(response.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_OK, data);

        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());
    }

    public void testFinishThreeDSecureVerificationDoesNothingWhenResultCodeNotOk()
            throws JSONException, InterruptedException {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        SimpleListener listener = new SimpleListener() {
            @Override
            public void onPaymentMethodsUpdated(List<PaymentMethod> paymentMethods) {
                wasCalled.set(true);
            }

            @Override
            public void onPaymentMethodCreated(PaymentMethod paymentMethod) {
                wasCalled.set(true);
            }

            @Override
            public void onPaymentMethodNonce(String paymentMethodNonce) {
                wasCalled.set(true);
            }

            @Override
            public void onUnrecoverableError(Throwable throwable) {
                wasCalled.set(true);
            }

            @Override
            public void onRecoverableError(ErrorWithResponse error) {
                wasCalled.set(true);
            }
        };
        mBraintree.addListener(listener);

        JSONObject card =
                new JSONObject(stringFromFixture(mContext, "payment_methods/visa_credit_card.json"));
        JSONObject json = new JSONObject()
                .put("success", true)
                .put("paymentMethod", card);

        Intent data = new Intent()
                .putExtra(ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT,
                        ThreeDSecureAuthenticationResponse.fromJson(json.toString()));

        mBraintree.finishThreeDSecureVerification(Activity.RESULT_CANCELED, data);

        waitForMainThreadToFinish();
        assertFalse(wasCalled.get());
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
        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        mBraintree.removeListener(listener);
        wasRemoved.set(true);

        mBraintree.getPaymentMethodsHelper().get();
        waitForMainThreadToFinish();
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
        waitForMainThreadToFinish();
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
        waitForMainThreadToFinish();
        assertFalse(wasCalled.get());

        hasListener.set(true);
        mBraintree.unlockListeners();
        waitForMainThreadToFinish();

        assertTrue("Expected callback to have been called", wasCalled.get());
    }

    public void testRemovedErrorListenerIsNotPostedTo()
            throws ExecutionException, InterruptedException, BraintreeException, ErrorWithResponse {
        BraintreeApi braintreeApi = unexpectedExceptionThrowingApi(getContext());
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

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
        waitForMainThreadToFinish();
        assertTrue(wasCalled.get());

        braintree.removeListener(listener);
        wasRemoved.set(true);

        braintree.getPaymentMethodsHelper().get();
        waitForMainThreadToFinish();
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
        waitForMainThreadToFinish();

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
        waitForMainThreadToFinish();
        assertEquals(0, callCount.get());

        braintree.unlockListeners();
        waitForMainThreadToFinish();
        assertEquals(1, callCount.get());

        braintree.unlockListeners();
        waitForMainThreadToFinish();
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
        waitForMainThreadToFinish();

        braintree.removeListener(firstListener);
        braintree.addListener(secondListener);
        braintree.unlockListeners();
        waitForMainThreadToFinish();

        assertFalse(firstListener.wasCalled());
        assertTrue(secondListener.wasCalled());
    }

    public void testProxiesSendAnalyticsEventToBraintreeApi()
            throws ExecutionException, InterruptedException {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        Braintree braintree = new Braintree(TEST_CLIENT_TOKEN_KEY, braintreeApi);

        verify(braintreeApi, never()).sendAnalyticsEvent(anyString(), anyString());
        braintree.sendAnalyticsEventHelper("event", "TEST").get();
        verify(braintreeApi, times(1)).sendAnalyticsEvent("event", "TEST");
    }

    public void testOnActivityResultHandlesPayPalResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishPayWithPayPal(any(Activity.class), anyInt(),
                any(Intent.class));
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION, "");

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).finishPayWithPayPal(null, responseCode, intent);
    }

    public void testOnActivityResultHandlesAndroidPayMaskedWalletResults() {
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_MASKED_WALLET, "");

        Braintree braintree = spy(mBraintree);
        doReturn(null).when(braintree).getAndroidPayGoogleTransactionId(intent);
        doNothing().when(braintree).performAndroidPayFullWalletRequest(any(Activity.class),
                anyInt(), any(Cart.class), anyString());

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).performAndroidPayFullWalletRequest(null, requestCode, null, null);
    }

    public void testOnActivityResultHandlesAndroidPayFullWalletResults() {
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_FULL_WALLET, "");

        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).getNonceFromAndroidPayFullWalletResponse(Activity.RESULT_OK,
                intent);

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).getNonceFromAndroidPayFullWalletResponse(responseCode, intent);
    }

    public void testOnActivityResultHandlesVenmoResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishPayWithVenmo(anyInt(), any(Intent.class));
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(AppSwitch.EXTRA_PAYMENT_METHOD_NONCE, "");

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).finishPayWithVenmo(responseCode, intent);
    }

    public void testOnActivityResultHandlesThreeDSecureResults() {
        Braintree braintree = spy(mBraintree);
        doNothing().when(braintree).finishThreeDSecureVerification(anyInt(), any(Intent.class));
        int requestCode = 10;
        int responseCode = Activity.RESULT_OK;
        Intent intent = new Intent().putExtra(
                ThreeDSecureWebViewActivity.EXTRA_THREE_D_SECURE_RESULT, "");

        braintree.onActivityResult(null, requestCode, responseCode, intent);

        verify(braintree).finishThreeDSecureVerification(responseCode, intent);
    }

    public void testOnActivityResultDoesNothingForUnknownActivity() {
        Braintree braintree = spy(mBraintree);
        int requestCode = 10;
        int resultCode = Activity.RESULT_OK;
        Intent intent = new Intent();

        braintree.onActivityResult(null, requestCode, resultCode, intent);

        verify(braintree).onActivityResult(null, requestCode, resultCode, intent);
        verifyNoMoreInteractions(braintree);
    }

    public void testOnResumeAddsListenersAndUnlocksListeners() {
        Braintree braintree = spy(new Braintree(null, mock(BraintreeApi.class)));
        Activity listenerActivity = mock(TestListenerActivity.class);

        braintree.onResume(listenerActivity);

        verify(braintree).addListener((PaymentMethodNonceListener) listenerActivity);
        verify(braintree).unlockListeners();
    }

    public void testOnPauseLocksListenersAndRemovesListeners() {
        Braintree braintree = spy(new Braintree(null, mock(BraintreeApi.class)));
        Activity listenerActivity = mock(TestListenerActivity.class);

        braintree.onPause(listenerActivity);

        verify(braintree).lockListeners();
        verify(braintree).removeListener((PaymentMethodNonceListener) listenerActivity);
    }

    public void testOnPauseDisconnectsGoogleApiClient() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        Braintree braintree = new Braintree(null, braintreeApi);

        braintree.onPause(null);

        verify(braintreeApi).disconnectGoogleApiClient();
    }

    public void testOnSaveInstanceStateAddsClientTokenAndConfigurationToBundle() {
        BraintreeApi braintreeApi = mock(BraintreeApi.class);
        when(braintreeApi.getConfigurationString()).thenReturn("configuration-string");
        Braintree braintree = new Braintree("client-token", braintreeApi);
        Bundle bundle = new Bundle();

        braintree.onSaveInstanceState(bundle);

        assertEquals("client-token", bundle.getString("com.braintreepayments.api.KEY_CLIENT_TOKEN"));
        assertEquals("configuration-string",
                bundle.getString("com.braintreepayments.api.KEY_CONFIGURATION"));
    }

    public void testReturnsNullIfBundleIsNull() {
        assertNull(Braintree.restoreSavedInstanceState(mContext, null));
    }

    public void testReturnsBraintreeInstanceIfThereIsAnExistingInstance() {
        Bundle bundle = new Bundle();

        mBraintree.onSaveInstanceState(bundle);
        Braintree restoredBraintree = Braintree.restoreSavedInstanceState(mContext, bundle);

        assertEquals(mBraintree, restoredBraintree);
    }

    public void testReturnsANewBraintreeInstanceIfThereIsNoExistingInstance() {
        Braintree.reset();
        Bundle bundle = new Bundle();
        bundle.putString("com.braintreepayments.api.KEY_CLIENT_TOKEN", "{}");
        bundle.putString("com.braintreepayments.api.KEY_CONFIGURATION", "{}");

        assertTrue(Braintree.sInstances.size() == 0);
        Braintree braintree = Braintree.restoreSavedInstanceState(mContext, bundle);
        assertTrue(braintree != null);
        assertTrue(Braintree.sInstances.size() == 1);
    }

    /** helper to synchronously create a credit card */
    private static void createCardSync(Braintree braintree) throws ExecutionException,
            InterruptedException {
        braintree.createHelper(new CardBuilder()
                .cardNumber(VISA)
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
