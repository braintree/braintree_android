package com.braintreepayments.api;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultRegistry;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;

import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureClientUnitTest {

    private FragmentActivity activity;
    private ActivityResultRegistry resultRegistry;
    private Lifecycle lifecycle;
    private ThreeDSecureAPI threeDSecureAPI;

    private ThreeDSecureListener listener;
    private ThreeDSecureResultCallback threeDSecureResultCallback;

    private Configuration threeDSecureEnabledConfig;

    ThreeDSecureRequest basicRequest;
    ThreeDSecureResult threeDSecureResult;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        threeDSecureResultCallback = mock(ThreeDSecureResultCallback.class);
        listener = mock(ThreeDSecureListener.class);
        threeDSecureAPI = mock(ThreeDSecureAPI.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal-jwt")
                .buildConfiguration();

        basicRequest = new ThreeDSecureRequest();
        basicRequest.setNonce("a-nonce");
        basicRequest.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        basicRequest.setBillingAddress(billingAddress);

        threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        lifecycle = mock(Lifecycle.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);
    }

    @Test
    public void performVerification_sendsAnalyticEvent() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.initialized");
    }

    @Test
    public void performVerification_sendsParamsInLookupRequest()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");
        request.setRequestedExemptionType(ThreeDSecureRequest.SECURE_CORPORATE);

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.performVerification(activity, request, threeDSecureResultCallback);

        String expectedUrl = "/v1/payment_methods/a-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(),
                any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(bodyCaptor.getValue());
        assertEquals("amount", body.getString("amount"));
        assertEquals("df-reference-id", body.getString("df_reference_id"));
        assertEquals("billing-given-name",
                body.getJSONObject("additional_info").getString("billing_given_name"));
        assertEquals("secure_corporate", body.getString("requested_exemption_type"));
    }

    @Test
    public void performVerification_performsLookup_WhenCardinalSDKInitFails()
            throws JSONException, BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));
        sut.performVerification(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(pathCaptor.capture(), bodyCaptor.capture(),
                any(HttpResponseCallback.class));

        String path = pathCaptor.getValue();
        String body = bodyCaptor.getValue();
        JSONObject bodyJson = new JSONObject(body);

        assertEquals("/v1/payment_methods/a-nonce/three_d_secure/lookup", path);
        assertEquals("amount", bodyJson.get("amount"));
        assertFalse(bodyJson.getBoolean("challenge_requested"));
        assertFalse(bodyJson.getBoolean("data_only_requested"));
        assertFalse(bodyJson.getBoolean("exemption_requested"));
        JSONObject additionalInfo = bodyJson.getJSONObject("additional_info");
        assertEquals("billing-given-name", additionalInfo.get("billing_given_name"));
    }

    @Test
    public void performVerification_callsLookupListener() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                .build();

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        new ThreeDSecureAPI(braintreeClient));

        sut.performVerification(activity, request, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(any(ThreeDSecureResult.class),
                (Exception) isNull());
    }

    @Test
    public void performVerification_withInvalidRequest_postsException() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.setListener(listener);

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setAmount("5");
        sut.performVerification(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());
        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenV1_throwsAnError() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        basicRequest.setVersionRequested(ThreeDSecureRequest.VERSION_1);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        ArgumentCaptor<BraintreeException> captor =
                ArgumentCaptor.forClass(BraintreeException.class);
        verify(threeDSecureResultCallback).onResult(isNull(), captor.capture());

        BraintreeException error = captor.getValue();
        String expectedMessage =
                "3D Secure v1 is deprecated and no longer supported. See https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4 for more information.";
        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    public void onActivityResult_whenResultNotOk_postsExceptionToCallback()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        verifyNoMoreInteractions(braintreeClient);
        sut.onActivityResult(AppCompatActivity.RESULT_CANCELED, new Intent(),
                threeDSecureResultCallback);
        verifyNoMoreInteractions(braintreeClient);

        ArgumentCaptor<BraintreeException> captor =
                ArgumentCaptor.forClass(BraintreeException.class);
        verify(threeDSecureResultCallback).onResult(isNull(),
                captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
        assertFalse(((UserCanceledException) exception).isExplicitCancelation());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_postsPayment() throws BraintreeException {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response",
                        Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<ThreeDSecureResult> captor =
                ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(listener).onThreeDSecureSuccess(captor.capture());
        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));

        ThreeDSecureResult result = captor.getValue();
        CardNonce cardNonce = result.getTokenizedCard();
        assertIsANonce(cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_sendAnalyticsEvents()
            throws BraintreeException {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response",
                        Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(browserSwitchResult);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shift-possible.true");
    }

    @Test
    public void onBrowserSwitchResult_whenFailure_postsErrorWithResponse() throws Exception {
        JSONObject json = new JSONObject();
        json.put("success", false);

        JSONObject errorJson = new JSONObject();
        errorJson.put("message", "Failed to authenticate, please try a different form of payment.");
        json.put("error", errorJson);

        Uri uri = Uri.parse("https://.com?auth_response=" + json.toString());

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<ErrorWithResponse> captor = ArgumentCaptor.forClass(ErrorWithResponse.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));

        ErrorWithResponse error = captor.getValue();
        assertEquals(422, error.getStatusCode());
        assertEquals("Failed to authenticate, please try a different form of payment.",
                error.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_returnsExceptionToCallback()
            throws BraintreeException {
        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.CANCELED, null, null);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
        assertFalse(((UserCanceledException) exception).isExplicitCancelation());
    }

    @Test
    public void onBrowserSwitchResult_whenListenerNull_setsPendingBrowserSwitchResult_andDoesNotDeliver()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.onBrowserSwitchResult(browserSwitchResult);

        assertSame(browserSwitchResult, sut.pendingBrowserSwitchResult);
        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));
    }

    @Test
    public void onCardinalResult_whenErrorExists_forwardsErrorToListener_andSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        Exception threeDSecureError = new Exception("3DS error.");
        CardinalResult cardinalResult = new CardinalResult(threeDSecureError);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureFailure(threeDSecureError);
    }

    @Test
    public void onCardinalResult_onSuccess_sendsAnalyticsEvent() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.cardinal-sdk.action-code.success");
    }

    @Test
    public void onCardinalResult_whenValidateResponseTimeout_returnsErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);
        when(validateResponse.getErrorDescription()).thenReturn("Error");

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Error", captor.getValue().getMessage());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseCancel_returnsUserCanceledErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
        assertTrue(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTResult_returnsResultAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback =
                        (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureSuccess(threeDSecureResult);
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.succeeded");
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.liability-shift-possible.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTResultWithError_returnsResultAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.hasError()).thenReturn(true);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback =
                        (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureSuccess(threeDSecureResult);
        verify(braintreeClient).sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTError_returnsErrorAndSendsAnalytics()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final Exception exception = new Exception("error");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback =
                        (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(null, exception);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(),
                any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult =
                new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureFailure(exception);
        braintreeClient.sendAnalyticsEvent(
                "three-d-secure.verification-flow.upgrade-payment-method.errored");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void constructor_setsLifecycleObserver() throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor =
                ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        ThreeDSecureLifecycleObserver observer = captor.getValue();
        assertSame(resultRegistry, observer.activityResultRegistry);
        assertSame(sut, observer.threeDSecureClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.requireActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);
        ThreeDSecureClient sut = new ThreeDSecureClient(fragment, braintreeClient);

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor =
                ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        ThreeDSecureLifecycleObserver observer = captor.getValue();
        assertSame(resultRegistry, observer.activityResultRegistry);
        assertSame(sut, observer.threeDSecureClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        when(activity.getLifecycle()).thenReturn(lifecycle);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, braintreeClient);

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor =
                ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        ThreeDSecureLifecycleObserver observer = captor.getValue();
        assertSame(resultRegistry, observer.activityResultRegistry);
        assertSame(sut, observer.threeDSecureClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient);

        verify(lifecycle, never()).addObserver(any(LifecycleObserver.class));
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultExists_deliversResultToListener_andSetsPendingResultNull()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response",
                        Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.pendingBrowserSwitchResult = browserSwitchResult;
        sut.setListener(listener);

        verify(listener).onThreeDSecureSuccess(any(ThreeDSecureResult.class));
        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultDoesNotExist_doesNotInvokeListener()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);
        sut.pendingBrowserSwitchResult = null;
        sut.setListener(listener);

        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));
        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void getBrowserSwitchResult_forwardsInvocationToBraintreeClient()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = mock(BraintreeClient.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        BrowserSwitchResult result = sut.getBrowserSwitchResult(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void deliverBrowserSwitchResult_forwardsInvocationToBraintreeClient()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = mock(BraintreeClient.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        BrowserSwitchResult result = sut.deliverBrowserSwitchResult(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void getBrowserSwitchResultFromCache_forwardsInvocationToBraintreeClient()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = mock(BraintreeClient.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(
                browserSwitchResult);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        BrowserSwitchResult result = sut.getBrowserSwitchResultFromNewTask(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void deliverBrowserSwitchResultFromNewTask_forwardsInvocationToBraintreeClient()
            throws BraintreeException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = mock(BraintreeClient.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(
                browserSwitchResult);

        ThreeDSecureClient sut =
                new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient,
                        threeDSecureAPI);

        BrowserSwitchResult result = sut.deliverBrowserSwitchResultFromNewTask(activity);
        assertSame(browserSwitchResult, result);
    }
}
