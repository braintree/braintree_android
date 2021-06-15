package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureClientUnitTest {

    private FragmentActivity activity;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;

    private ThreeDSecureResultCallback threeDSecureResultCallback;

    private Configuration threeDSecureEnabledConfig;

    ThreeDSecureRequest basicRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        threeDSecureResultCallback = mock(ThreeDSecureResultCallback.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);

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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.performVerification(activity, request, threeDSecureResultCallback);

        String expectedUrl = "/v1/payment_methods/a-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HTTPResponseCallback.class));

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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.performVerification(activity, request, threeDSecureResultCallback);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(pathCaptor.capture(), bodyCaptor.capture(), any(HTTPResponseCallback.class));

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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performVerification(activity, request, callback);

        verify(callback).onResult(any(ThreeDSecureResult.class), any(Exception.class));
    }

    @Test
    public void performVerification_withInvalidRequest_postsException() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);

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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
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

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.performVerification(activity, basicRequest, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.invalid-manifest");
    }

    @Test
    public void onActivityResult_whenResultNotOk_postsExceptionToCallback() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);

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
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(browserSwitchResult, threeDSecureResultCallback);

        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(threeDSecureResultCallback).onResult(captor.capture(), (Exception) isNull());

        ThreeDSecureResult result = captor.getValue();
        CardNonce cardNonce = result.getTokenizedCard();
        assertIsANonce(cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertTrue(cardNonce.getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_sendAnalyticsEvents() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(browserSwitchResult, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shift-possible.true");
    }

    @Test
    public void onBrowserSwitchResult_whenFailure_postsErrorWithResponse() throws Exception {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        JSONObject json = new JSONObject();
        json.put("success", false);

        JSONObject errorJson = new JSONObject();
        errorJson.put("message", "Failed to authenticate, please try a different form of payment.");
        json.put("error", errorJson);

        Uri uri = Uri.parse("https://.com?auth_response=" + json.toString());

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.SUCCESS, null, uri);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(browserSwitchResult, threeDSecureResultCallback);

        ArgumentCaptor<ErrorWithResponse> captor = ArgumentCaptor.forClass(ErrorWithResponse.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        ErrorWithResponse error = captor.getValue();
        assertEquals(422, error.getStatusCode());
        assertEquals("Failed to authenticate, please try a different form of payment.", error.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchResultIsNull_returnsExceptionToCallback() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(null, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("BrowserSwitchResult cannot be null", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_returnsExceptionToCallback() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        BrowserSwitchResult browserSwitchResult =
                new BrowserSwitchResult(BrowserSwitchStatus.CANCELED, null, null);

        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(browserSwitchResult, threeDSecureResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureResultCallback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled 3DS.", exception.getMessage());
    }
}
