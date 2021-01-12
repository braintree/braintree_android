package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.MockBraintreeClientBuilder;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.helpers.MockCardinalClientBuilder;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupCallback;
import com.braintreepayments.api.internal.ThreeDSecureV1BrowserSwitchHelper;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.braintreepayments.testutils.Fixtures;
import com.braintreepayments.testutils.TestConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.Assertions.assertIsANonce;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureUnitTest {

    private FragmentActivity activity;
    private ThreeDSecureV1BrowserSwitchHelper browserSwitchHelper;

    private ThreeDSecureVerificationCallback threeDSecureVerificationCallback;
    private ThreeDSecureResultCallback threeDSecureResultCallback;

    private Configuration threeDSecureEnabledConfig;

    ThreeDSecureRequest basicRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        threeDSecureVerificationCallback = mock(ThreeDSecureVerificationCallback.class);
        threeDSecureResultCallback = mock(ThreeDSecureResultCallback.class);
        browserSwitchHelper = mock(ThreeDSecureV1BrowserSwitchHelper.class);

        threeDSecureEnabledConfig = new TestConfigurationBuilder()
                .threeDSecureEnabled(true)
                .cardinalAuthenticationJWT("cardinal-jwt")
                .buildConfiguration();

        basicRequest = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));
    }

    @Test
    public void performVerification_sendsAnalyticEvent() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, basicRequest, threeDSecureVerificationCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.initialized");
    }

    @Test
    public void performVerification_sendsParamsInLookupRequest() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("df-reference-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, request, threeDSecureVerificationCallback);

        String expectedUrl = "/v1/payment_methods/a-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(bodyCaptor.getValue());
        assertEquals("amount", body.getString("amount"));
        assertEquals("df-reference-id", body.getString("df_reference_id"));
        assertEquals("billing-given-name", body.getJSONObject("additional_info").getString("billing_given_name"));
    }

    @Test
    public void performVerification_performsLookup_WhenCardinalSDKInitFails() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .error(new Exception("error"))
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, request, threeDSecureVerificationCallback);

        // TODO: consider refining this assertion to be more precise than the original
        verify(braintreeClient).sendPOST(anyString(), anyString(), any(HttpResponseCallback.class));
    }

    @Test
    public void performVerification_withCardBuilder_errorsWhenNoAmount() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest();

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, cardBuilder, request, threeDSecureVerificationCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureVerificationCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        assertEquals("The ThreeDSecureRequest amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_withCardBuilderFailsToTokenize_postsError() {
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        Exception tokenizationError = new Exception("error");
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .error(tokenizationError)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, cardBuilder, request, threeDSecureVerificationCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureVerificationCallback).onResult((PaymentMethodNonce) isNull(), exceptionCaptor.capture());

        assertSame(tokenizationError, exceptionCaptor.getValue());
    }

    @Test
    public void performVerification_withCardBuilder_tokenizesAndPerformsVerification() throws JSONException {
        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        PaymentMethodNonce paymentMethodNonce = mock(PaymentMethodNonce.class);
        when(paymentMethodNonce.getNonce()).thenReturn("card-nonce");

        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .successNonce(paymentMethodNonce)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        CardBuilder cardBuilder = new CardBuilder();
        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .amount("10");

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, cardBuilder, request, threeDSecureVerificationCallback);

        String expectedUrl = "/v1/payment_methods/card-nonce/three_d_secure/lookup";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HttpResponseCallback.class));

        JSONObject body = new JSONObject(bodyCaptor.getValue());
        assertEquals("10", body.getString("amount"));
    }

    @Test
    public void performVerification_callsLookupListener() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        CardinalClient cardinalClient = new MockCardinalClientBuilder()
                .successReferenceId("sample-session-id")
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        when(browserSwitchHelper.getUrl(anyString(), anyString(), any(ThreeDSecureRequest.class), any(ThreeDSecureLookup.class))).thenReturn("https://example.com");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .amount("amount")
                .billingAddress(new ThreeDSecurePostalAddress()
                        .givenName("billing-given-name"));

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureLookupCallback lookupListener = mock(ThreeDSecureLookupCallback.class);
        sut.performVerification(activity, request, lookupListener);

        verify(lookupListener).onResult(same(request), any(ThreeDSecureLookup.class), any(Exception.class));
    }

    @Test
    public void performVerification_withInvalidRequest_postsException() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(true);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        ThreeDSecureRequest request = new ThreeDSecureRequest().amount("5");
        sut.performVerification(activity, request, threeDSecureVerificationCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureVerificationCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());
        assertEquals("The ThreeDSecureRequest nonce and amount cannot be null",
                captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_postsException() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(false);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, basicRequest, threeDSecureVerificationCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(threeDSecureVerificationCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        assertEquals("BraintreeBrowserSwitchActivity missing, " +
                "incorrectly configured in AndroidManifest.xml or another app defines the same browser " +
                "switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/v2#browser-switch " +
                "for the correct configuration", captor.getValue().getMessage());
    }

    @Test
    public void performVerification_whenBrowserSwitchNotSetup_sendsAnalyticEvent() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(threeDSecureEnabledConfig)
                .build();
        when(braintreeClient.canPerformBrowserSwitch(activity, BraintreeRequestCodes.THREE_D_SECURE)).thenReturn(false);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.performVerification(activity, basicRequest, threeDSecureVerificationCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.invalid-manifest");
    }

    @Test
    public void onActivityResult_whenResultNotOk_doesNothing() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);

        verifyNoMoreInteractions(braintreeClient);
        sut.onActivityResult(activity, AppCompatActivity.RESULT_CANCELED, new Intent(), threeDSecureResultCallback);
        verifyNoMoreInteractions(braintreeClient);
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_postsPayment() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
            new BrowserSwitchResult(BrowserSwitchResult.STATUS_OK, null);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(activity, browserSwitchResult, uri, threeDSecureResultCallback);

        ArgumentCaptor<PaymentMethodNonce> captor = ArgumentCaptor.forClass(PaymentMethodNonce.class);
        verify(threeDSecureResultCallback).onResult(captor.capture(), (Exception) isNull());

        PaymentMethodNonce paymentMethodNonce = captor.getValue();
        assertIsANonce(paymentMethodNonce.getNonce());
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
        assertTrue(((CardNonce) paymentMethodNonce).getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void onBrowserSwitchResult_whenSuccessful_sendAnalyticsEvents() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Uri uri = Uri.parse("http://demo-app.com")
                .buildUpon()
                .appendQueryParameter("auth_response", Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();

        BrowserSwitchResult browserSwitchResult =
            new BrowserSwitchResult(BrowserSwitchResult.STATUS_OK, null);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(activity, browserSwitchResult, uri, threeDSecureResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shifted.true");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.liability-shift-possible.true");
    }

    @Test
    public void onBrowserSwitchResult_whenFailure_postsErrorWithResponse() throws Exception {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        JSONObject json = new JSONObject();
        json.put("success", false);

        JSONObject errorJson = new JSONObject();
        errorJson.put("message", "Failed to authenticate, please try a different form of payment.");
        json.put("error", errorJson);

        Uri uri = Uri.parse("https://.com?auth_response=" + json.toString());

        BrowserSwitchResult browserSwitchResult =
            new BrowserSwitchResult(BrowserSwitchResult.STATUS_OK, null);

        ThreeDSecure sut = new ThreeDSecure(braintreeClient, "sample-scheme", cardinalClient, tokenizationClient, browserSwitchHelper);
        sut.onBrowserSwitchResult(activity, browserSwitchResult, uri, threeDSecureResultCallback);

        ArgumentCaptor<ErrorWithResponse> captor = ArgumentCaptor.forClass(ErrorWithResponse.class);
        verify(threeDSecureResultCallback).onResult((PaymentMethodNonce) isNull(), captor.capture());

        ErrorWithResponse error = captor.getValue();
        assertEquals(422, error.getStatusCode());
        assertEquals("Failed to authenticate, please try a different form of payment.", error.getMessage());
    }
}