package com.braintreepayments.api;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class UnionPayClientUnitTest {

    private UnionPayTokenizeCallback unionPayTokenizeCallback;

    private BraintreeClient braintreeClient;
    private ApiClient apiClient;

    private UnionPayCard unionPayCard;
    private UnionPayEnrollCallback unionPayEnrollCallback;
    private UnionPayFetchCapabilitiesCallback unionPayFetchCapabilitiesCallback;

    private Configuration unionPayEnabledConfiguration;
    private Configuration unionPayDisabledConfiguration;

    @Before
    public void beforeEach() throws JSONException {
        unionPayTokenizeCallback = mock(UnionPayTokenizeCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        apiClient = mock(ApiClient.class);

        unionPayEnabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_UNIONPAY);
        unionPayDisabledConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        unionPayCard = mock(UnionPayCard.class);
        unionPayEnrollCallback = mock(UnionPayEnrollCallback.class);
        unionPayFetchCapabilitiesCallback = mock(UnionPayFetchCapabilitiesCallback.class);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnTokenizeResult() throws JSONException {
        UnionPayCard unionPayCard = new UnionPayCard();
        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.tokenize(unionPayCard, unionPayTokenizeCallback);

        ArgumentCaptor<TokenizeCallback> captor = ArgumentCaptor.forClass(TokenizeCallback.class);
        verify(apiClient).tokenizeREST(same(unionPayCard), captor.capture());

        TokenizeCallback callback = captor.getValue();
        callback.onResult(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD), null);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.nonce-received");
    }

    @Test
    public void tokenize_callsListenerWithErrorOnFailure() {
        UnionPayCard unionPayCard = new UnionPayCard();
        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.tokenize(unionPayCard, unionPayTokenizeCallback);

        ArgumentCaptor<TokenizeCallback> captor = ArgumentCaptor.forClass(TokenizeCallback.class);
        verify(apiClient).tokenizeREST(same(unionPayCard), captor.capture());

        TokenizeCallback callback = captor.getValue();
        Exception error = new ErrorWithResponse(422, "");
        callback.onResult(null, error);

        verify(unionPayTokenizeCallback).onResult(null, error);
    }

    @Test
    public void tokenize_sendsAnalyticsEventOnFailure() {
        UnionPayCard unionPayCard = new UnionPayCard();
        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.tokenize(unionPayCard, unionPayTokenizeCallback);

        ArgumentCaptor<TokenizeCallback> captor = ArgumentCaptor.forClass(TokenizeCallback.class);
        verify(apiClient).tokenizeREST(same(unionPayCard), captor.capture());

        TokenizeCallback callback = captor.getValue();
        Exception error = new ErrorWithResponse(422, "");
        callback.onResult(null, error);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.nonce-failed");
    }

    @Test
    public void enroll_sendsPOSTRequestForEnrollment() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .build();

        String unionPayCardJson = "{\"sample\":\"json\"}";
        when(unionPayCard.buildEnrollment()).thenReturn(new JSONObject(unionPayCardJson));

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.enroll(unionPayCard, unionPayEnrollCallback);

        String expectedPath = "/v1/union_pay_enrollments";
        verify(braintreeClient).sendPOST(eq(expectedPath), eq(unionPayCardJson), any(HTTPResponseCallback.class));
    }

    @Test
    public void enroll_callsListenerWithUnionPayEnrollmentIdAdded() throws JSONException {
        String response = new JSONObject()
                .put("unionPayEnrollmentId", "some-enrollment-id")
                .put("smsCodeRequired", true)
                .toString();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendPOSTSuccessfulResponse(response)
                .build();

        when(unionPayCard.buildEnrollment()).thenReturn(new JSONObject("{}"));

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.enroll(unionPayCard, unionPayEnrollCallback);

        ArgumentCaptor<UnionPayEnrollment> resultCaptor = ArgumentCaptor.forClass(UnionPayEnrollment.class);
        verify(unionPayEnrollCallback).onResult(resultCaptor.capture(), (Exception) isNull());

        UnionPayEnrollment result = resultCaptor.getValue();
        assertEquals("some-enrollment-id", result.getId());
        assertTrue(result.isSmsCodeRequired());
    }

    @Test
    public void enroll_failsIfUnionPayIsDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayDisabledConfiguration)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.enroll(unionPayCard, unionPayEnrollCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(unionPayEnrollCallback).onResult((UnionPayEnrollment) isNull(), exceptionCaptor.capture());

        Exception exception = exceptionCaptor.getValue();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("UnionPay is not enabled", exception.getMessage());
    }

    @Test
    public void enroll_sendsAnalyticsEventOnFailure() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendPOSTErrorResponse(new BraintreeException())
                .build();

        when(unionPayCard.buildEnrollment()).thenReturn(new JSONObject("{}"));

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.enroll(unionPayCard, unionPayEnrollCallback);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.enrollment-failed");
    }

    @Test
    public void enroll_sendsAnalyticsEventOnSuccess() throws JSONException {
        String response = new JSONObject()
                .put("unionPayEnrollmentId", "some-enrollment-id")
                .put("smsCodeRequired", true)
                .toString();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendPOSTSuccessfulResponse(response)
                .build();

        when(unionPayCard.buildEnrollment()).thenReturn(new JSONObject("{}"));

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.enroll(unionPayCard, unionPayEnrollCallback);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.enrollment-succeeded");
    }

    @Test
    public void fetchCapabilities_sendsPayloadToEndpoint() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        String expectedUrl = Uri.parse("/v1/payment_methods/credit_cards/capabilities")
                .buildUpon()
                .appendQueryParameter("creditCard[number]", CardNumber.UNIONPAY_CREDIT)
                .build()
                .toString();

        verify(braintreeClient).sendGET(eq(expectedUrl), any(HTTPResponseCallback.class));
    }

    @Test
    public void fetchCapabilities_callsListenerWithErrorOnFailure() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendGETErrorResponse(error)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        verify(unionPayFetchCapabilitiesCallback).onResult((UnionPayCapabilities) isNull(), same(error));
    }

    @Test
    public void fetchCapabilities_sendsAnalyticsEventOnFailure() {
        Exception error = new Exception("error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendGETErrorResponse(error)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.capabilities-failed");
    }

    @Test
    public void fetchCapabilities_callsListenerWithCapabilitiesOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendGETSuccessfulResponse(Fixtures.UNIONPAY_CAPABILITIES_SUCCESS_RESPONSE)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        ArgumentCaptor<UnionPayCapabilities> capabilitiesCaptor = ArgumentCaptor.forClass(UnionPayCapabilities.class);
        verify(unionPayFetchCapabilitiesCallback).onResult(capabilitiesCaptor.capture(), (Exception) isNull());

        UnionPayCapabilities capabilities = capabilitiesCaptor.getValue();
        assertNotNull(capabilities);
        assertTrue(capabilities.isUnionPay());
        assertFalse(capabilities.isDebit());
        assertTrue(capabilities.supportsTwoStepAuthAndCapture());
        assertTrue(capabilities.isSupported());
    }

    @Test
    public void fetchCapabilities_sendsAnalyticsEventOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayEnabledConfiguration)
                .sendGETSuccessfulResponse(Fixtures.UNIONPAY_CAPABILITIES_SUCCESS_RESPONSE)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        verify(braintreeClient).sendAnalyticsEvent("union-pay.capabilities-received");
    }

    @Test
    public void fetchCapabilities_failsIfUnionPayIsDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(unionPayDisabledConfiguration)
                .build();

        UnionPayClient sut = new UnionPayClient(braintreeClient, apiClient);
        sut.fetchCapabilities(CardNumber.UNIONPAY_CREDIT, unionPayFetchCapabilitiesCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(unionPayFetchCapabilitiesCallback).onResult((UnionPayCapabilities) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("UnionPay is not enabled", exception.getMessage());
    }
}