package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;

public class ThreeDSecureAPIUnitTest {

    private ThreeDSecureAPI sut;

    @Test
    public void performLookup_sendsPOSTRequest() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureRequest threeDSecureRequest = mock(ThreeDSecureRequest.class);
        String mockData = "{\"mock\":\"json\"}";
        when(threeDSecureRequest.getNonce()).thenReturn("sample-nonce");
        when(threeDSecureRequest.build("cardinal-session-id")).thenReturn(mockData);

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(urlCaptor.capture(), dataCaptor.capture(), any(HttpResponseCallback.class));

        String url = urlCaptor.getValue();
        assertEquals("/v1/payment_methods/sample-nonce/three_d_secure/lookup", url);

        String data = dataCaptor.getValue();
        assertSame(mockData, data);
    }

    @Test
    public void performLookup_onSuccess_callbackThreeDSecureResult() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureRequest threeDSecureRequest = mock(ThreeDSecureRequest.class);
        when(threeDSecureRequest.build(anyString())).thenReturn("{}");

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performLookup(threeDSecureRequest, "another-session-id", callback);

        verify(callback).onResult(any(ThreeDSecureResult.class), (Exception) isNull());
    }

    @Test
    public void performLookup_onInvalidJSONResponse_callbackJSONException() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse("invalid json")
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureRequest threeDSecureRequest = mock(ThreeDSecureRequest.class);
        when(threeDSecureRequest.build(anyString())).thenReturn("{}");

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback);

        verify(callback).onResult((ThreeDSecureResult) isNull(), any(JSONException.class));
    }

    @Test
    public void performLookup_onPOSTFailure_callbackHTTPError() {
        Exception httpError = new Exception("http error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTErrorResponse(httpError)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureRequest threeDSecureRequest = mock(ThreeDSecureRequest.class);
        when(threeDSecureRequest.build(anyString())).thenReturn("{}");

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.performLookup(threeDSecureRequest, "cardinal-session-id", callback);

        verify(callback).onResult((ThreeDSecureResult) isNull(), same(httpError));
    }

    @Test
    public void authenticateCardinalJWT_sendsPOSTRequest() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(urlCaptor.capture(), dataCaptor.capture(), any(HttpResponseCallback.class));

        String url = urlCaptor.getValue();
        assertEquals("/v1/payment_methods/123456-12345-12345-a-adfa/three_d_secure/authenticate_from_jwt", url);

        String data = dataCaptor.getValue();
        JSONObject expectedJSON = new JSONObject()
                .put("jwt", "cardinal-jwt")
                .put("paymentMethodNonce", "123456-12345-12345-a-adfa");
        JSONAssert.assertEquals(expectedJSON, new JSONObject(data), true);
    }

    @Test
    public void authenticateCardinalJWT_onSuccess_callbackThreeDSecureResult() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        verify(callback).onResult(any(ThreeDSecureResult.class), (Exception) isNull());
    }

    @Test
    public void authenticateCardinalJWT_onThreeDSecureError_callbackThreeDSecureResultWithOriginalLookupNonce() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE_WITH_ERROR)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        ArgumentCaptor<ThreeDSecureResult> captor =
                ArgumentCaptor.forClass(ThreeDSecureResult.class);
        verify(callback).onResult(captor.capture(), (Exception) isNull());

        ThreeDSecureResult result = captor.getValue();
        assertNotNull(result.getTokenizedCard());
    }

    @Test
    public void authenticateCardinalJWT_onInvalidJSONResponse_callbackJSONException() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse("not-json")
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        ArgumentCaptor<Exception> captor =
                ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        Exception error = captor.getValue();
        assertTrue(error instanceof JSONException);
    }

    @Test
    public void authenticateCardinalJWT_onPOSTFailure_callbackHTTPError() throws JSONException {
        Exception postError = new Exception("post-error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTErrorResponse(postError)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult =
                ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        ArgumentCaptor<Exception> captor =
                ArgumentCaptor.forClass(Exception.class);
        verify(callback).onResult((ThreeDSecureResult) isNull(), captor.capture());

        Exception error = captor.getValue();
        assertSame(postError, error);
    }

    @Test
    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_sendsAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR)
                .build();
        sut = new ThreeDSecureAPI(braintreeClient);

        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        String cardinalJWT = "cardinal-jwt";

        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
        sut.authenticateCardinalJWT(threeDSecureResult, cardinalJWT, callback);

        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
    }
    // TODO - Make sure these tests are covered
//
//    @Test
//    public void authenticateCardinalJWT_whenSuccess_sendsAnalyticsEvent() throws JSONException {
//        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
//
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
//                .configuration(threeDSecureEnabledConfig)
//                .sendPOSTSuccessfulResponse(Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE)
//                .build();
//        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
//
//        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
//
//        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
//        sut.authenticateCardinalJWT(threeDSecureResult, "jwt", mock(ThreeDSecureResultCallback.class));
//
//        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
//        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
//    }
//
//    @Test
//    public void authenticateCardinalJWT_whenCustomerFailsAuthentication_returnsLookupCardNonce() throws JSONException {
//        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
//
//        String authResponseJson = Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR;
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
//                .configuration(threeDSecureEnabledConfig)
//                .sendPOSTSuccessfulResponse(authResponseJson)
//                .build();
//        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
//
//        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(
//                Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_WITHOUT_LIABILITY_WITH_LIABILITY_SHIFT_POSSIBLE);
//
//        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
//
//        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
//        sut.authenticateCardinalJWT(threeDSecureResult, "jwt", callback);
//
//        ArgumentCaptor<ThreeDSecureResult> captor = ArgumentCaptor.forClass(ThreeDSecureResult.class);
//        verify(callback).onResult(captor.capture(), (Exception) isNull());
//
//        ThreeDSecureResult actualResult = captor.getValue();
//        CardNonce cardNonce = actualResult.getTokenizedCard();
//        assertNotNull(cardNonce);
//
//        ThreeDSecureInfo threeDSecureInfo = cardNonce.getThreeDSecureInfo();
//        assertFalse(threeDSecureInfo.isLiabilityShifted());
//        assertTrue(threeDSecureInfo.isLiabilityShiftPossible());
//        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
//        assertEquals("Failed to authenticate, please try a different form of payment.", actualResult.getErrorMessage());
//    }
//
//    @Test
//    public void authenticateCardinalJWT_whenExceptionOccurs_sendsAnalyticsEvent() throws JSONException {
//        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
//
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
//                .configuration(threeDSecureEnabledConfig)
//                .sendPOSTErrorResponse(new BraintreeException("an error occurred!"))
//                .build();
//        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
//
//        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
//
//        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
//
//        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
//        sut.authenticateCardinalJWT(threeDSecureResult, "jwt", callback);
//
//        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.started");
//        verify(braintreeClient).sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
//    }
//
//    @Test
//    public void authenticateCardinalJWT_whenExceptionOccurs_returnsException() throws JSONException {
//        CardinalClient cardinalClient = new MockCardinalClientBuilder().build();
//
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//                .authorizationSuccess(Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN))
//                .configuration(threeDSecureEnabledConfig)
//                .sendPOSTErrorResponse(new BraintreeException("an error occurred!"))
//                .build();
//        when(braintreeClient.canPerformBrowserSwitch(activity, THREE_D_SECURE)).thenReturn(true);
//
//        ThreeDSecureResult threeDSecureResult = ThreeDSecureResult.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
//
//        ThreeDSecureClient sut = new ThreeDSecureClient(braintreeClient, cardinalClient, browserSwitchHelper);
//
//        ThreeDSecureResultCallback callback = mock(ThreeDSecureResultCallback.class);
//        sut.authenticateCardinalJWT(threeDSecureResult, "jwt", callback);
//
//        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
//        verify(callback).onResult((ThreeDSecureResult) isNull(), captor.capture());
//
//        assertEquals("an error occurred!", captor.getValue().getMessage());
//    }
}
