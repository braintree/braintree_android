package com.braintreepayments.api;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
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
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;
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
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);
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

        threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);

        resultRegistry = mock(ActivityResultRegistry.class);
        when(activity.getActivityResultRegistry()).thenReturn(resultRegistry);

        lifecycle = mock(Lifecycle.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);
    }

    @Test
    public void performVerification_sendsAnalyticEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.initialized");
    }

    @Test
    public void performVerification_sendsParamsInLookupRequest() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.performVerification(activity, request, threeDSecureResultCallback);

        String expectedUrl = "/v1/payment_methods/a-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(bodyCaptor.getValue());
        assertEquals("amount", body.getString("amount"));
        assertEquals("df-reference-id", body.getString("df_reference_id"));
        assertEquals("billing-given-name", body.getJSONObject("additional_info").getString("billing_given_name"));
    }

    @Test
    public void performVerification_performsLookup_WhenCardinalSDKInitFails() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));
        sut.performVerification(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(pathCaptor.capture(), bodyCaptor.capture(), any(HttpResponseCallback.class));

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
    public void performVerification_callsLookupListener() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setNonce("a-nonce");
        request.setVersionRequested(ThreeDSecureRequest.VERSION_2);
        request.setAmount("amount");

        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress();
        billingAddress.setGivenName("billing-given-name");
        request.setBillingAddress(billingAddress);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, new ThreeDSecureAPI(braintreeClient));

        sut.performVerification(activity, request, threeDSecureResultCallback);

        verify(threeDSecureResultCallback).onResult(any(ThreeDSecureResult.class), (Exception) isNull());
    }

    @Test
    public void performVerification_withInvalidRequest_postsException() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);

        ThreeDSecureRequest request = new ThreeDSecureRequest();
        request.setAmount("5");
        sut.performVerification(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());
        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_postsException() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(false);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_sendsAnalyticEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(false);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.invalid-manifest");
    }

    @Test
    public void onActivityResult_whenResultNotOk_postsExceptionToCallback() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);

        verifyNoMoreInteractions(braintreeClient);
        sut.onActivityResult(AppCompatActivity.RESULT_CANCELED, new Intent(), threeDSecureResultCallback);
        verifyNoMoreInteractions(braintreeClient);

        ArgumentCaptor<BraintreeException> captor = ArgumentCaptor.forClass(BraintreeException.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_postsPayment() {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(listener).onThreeDSecureSuccess(captor.capture());
        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));

        ThreeDSecureResult result = captor.getValue();
        CardNonce cardNonce = result.getTokenizedCard();
        assertIsANonce(cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_sendAnalyticsEvents() {
        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(activity);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shift-possible.true");
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
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<ErrorWithResponse> captor = ArgumentCaptor.forClass(ErrorWithResponse.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));

        ErrorWithResponse error = captor.getValue();
        assertEquals(422, error.getStatusCode());
        assertEquals("Failed to authenticate, please try a different form of payment.", error.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchResultIsNull_doesNothing() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(null)
                .build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(activity);

        verify(listener, never()).onThreeDSecureFailure(any(Exception.class));
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_returnsExceptionToCallback() {
        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.CANCELED, null, null);

        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        sut.setListener(listener);
        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        verify(listener, never()).onThreeDSecureSuccess(any(ThreeDSecureResult.class));

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenListenerNull_doesNothing() {
        // We haven't implemented this behavior - what is our expectation around what should happen
        // when the listener is null? Should we fail silently and avoid an NPE or should we let the
        // app crash to alert the merchant of a setup problem?
    }

    @Test
    public void onCardinalResult_whenErrorExists_forwardsErrorToListener_andSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        Exception threeDSecureError = new Exception("3DS error.");
        CardinalResult cardinalResult = new CardinalResult(threeDSecureError);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureFailure(threeDSecureError);
    }

    @Test
    public void onCardinalResult_onSuccess_sendsAnalyticsEvent() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.cardinal-sdk.action-code.success");
    }

    @Test
    public void onCardinalResult_whenValidateResponseTimeout_returnsErrorAndSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.TIMEOUT);
        when(validateResponse.getErrorDescription()).thenReturn("Error");

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("Error", captor.getValue().getMessage());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.failed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseCancel_returnsUserCanceledErrorAndSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.CANCEL);

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onThreeDSecureFailure(captor.capture());
        assertTrue(captor.getValue() instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", captor.getValue().getMessage());

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.canceled");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTResult_returnsResultAndSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(), any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureSuccess(threeDSecureResult);
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shift-possible.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTResultWithError_returnsResultAndSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final ThreeDSecureResult threeDSecureResult = mock(ThreeDSecureResult.class);
        when(threeDSecureResult.hasError()).thenReturn(true);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(threeDSecureResult, null);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(), any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureSuccess(threeDSecureResult);
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void onCardinalResult_whenValidateResponseSuccess_onAuthenticateCardinalJWTError_returnsErrorAndSendsAnalytics() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ValidateResponse validateResponse = mock(ValidateResponse.class);
        when(validateResponse.getActionCode()).thenReturn(CardinalActionCode.SUCCESS);

        final Exception exception = new Exception("error");

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ThreeDSecureResultCallback callback = (ThreeDSecureResultCallback) invocation.getArguments()[2];
                callback.onResult(null, exception);
                return null;
            }
        }).when(threeDSecureAPI).authenticateCardinalJWT(any(ThreeDSecureResult.class), anyString(), any(ThreeDSecureResultCallback.class));

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);
        ThreeDSecureListener listener = mock(ThreeDSecureListener.class);
        sut.setListener(listener);

        CardinalResult cardinalResult = new CardinalResult(threeDSecureResult, "jwt", validateResponse);
        sut.onCardinalResult(cardinalResult);

        verify(listener).onThreeDSecureFailure(exception);
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.completed");
    }

    @Test
    public void constructor_setsLifecycleObserver() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(activity, lifecycle, braintreeClient, cardinalClient, browserSwitchHelper, threeDSecureAPI);

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor = ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
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

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor = ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
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

        ArgumentCaptor<ThreeDSecureLifecycleObserver> captor = ArgumentCaptor.forClass(ThreeDSecureLifecycleObserver.class);
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
}
