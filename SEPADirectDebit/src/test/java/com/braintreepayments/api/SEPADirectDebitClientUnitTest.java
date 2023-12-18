package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADirectDebitClientUnitTest {

    private BraintreeClient braintreeClient;
    private CreateMandateResult createMandateResult;
    private SEPADirectDebitRequest sepaDirectDebitRequest;
    private SEPADirectDebitTokenizeCallback sepaTokenizeCallback;
    private SEPADirectDebitPaymentAuthRequestCallback paymentAuthRequestCallback;

    @Before
    public void beforeEach() {
        braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("com.example")
                .build();

        createMandateResult = new CreateMandateResult(
                "http://www.example.com",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );
        sepaDirectDebitRequest = new SEPADirectDebitRequest();

        sepaTokenizeCallback = mock(SEPADirectDebitTokenizeCallback.class);
        paymentAuthRequestCallback = mock(SEPADirectDebitPaymentAuthRequestCallback.class);
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_callsBackSEPAResponse_andSendsAnalytics()
            throws JSONException {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, paymentAuthRequestCallback);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.selected.started");

        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onResult(captor.capture());

        SEPADirectDebitPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.ReadyToLaunch);
        SEPADirectDebitPaymentAuthRequestParams params = ((SEPADirectDebitPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();

        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.requested");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");

        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();
        assertEquals(Uri.parse("http://www.example.com"), browserSwitchOptions.getUrl());
        assertEquals("com.example", browserSwitchOptions.getReturnUrlScheme());
        assertEquals(BraintreeRequestCodes.SEPA_DEBIT, browserSwitchOptions.getRequestCode());
        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("1234", metadata.get("ibanLastFour"));
        assertEquals("fake-customer-id", metadata.get("customerId"));
        assertEquals("fake-bank-reference-token", metadata.get("bankReferenceToken"));
        assertEquals("ONE_OFF", metadata.get("mandateType"));
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenMandateAlreadyApproved_onTokenizeSuccess_callsBackResultWithNonce_andSendsAnalytics()
            throws JSONException {
        // null approval URL indicates mandate approved
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(
                new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .tokenizeSuccess(nonce)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, paymentAuthRequestCallback);

        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                SEPADirectDebitPaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onResult(captor.capture());

        SEPADirectDebitPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.LaunchNotRequired);
        assertEquals(((SEPADirectDebitPaymentAuthRequest.LaunchNotRequired) paymentAuthRequest).getNonce(), nonce);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
    }

    @Test
    public void createPaymentAuthRequest_onCreateMandateRequestSuccess_whenApprovalURLInvalid_callsBackError() {
        createMandateResult = new CreateMandateResult(
                "",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, paymentAuthRequestCallback);

        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor = ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onResult(captor.capture());

        SEPADirectDebitPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.Failure);
        Exception error = ((SEPADirectDebitPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(error instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", error.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLNull_callsSEPADirectDebitAPI_tokenize_andSendsAnalytics()
            throws BrowserSwitchException {
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, paymentAuthRequestCallback);
        verify(braintreeClient, never()).startBrowserSwitch(any(FragmentActivity.class),
                any(BrowserSwitchOptions.class));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("fake-customer-id"),
                eq("fake-bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitInternalTokenizeCallback.class));
    }

    @Test
    public void tokenize_onCreateMandateError_returnsErrorToListener_andSendsAnalytics() {
        Exception error = new Exception("error");
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateError(error)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, paymentAuthRequestCallback);
        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor = ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthRequest.class);
        verify(paymentAuthRequestCallback).onResult(captor.capture());

        SEPADirectDebitPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof SEPADirectDebitPaymentAuthRequest.Failure);
        Exception actualError = ((SEPADirectDebitPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertEquals(error, actualError);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");
    }

    @Test
    public void onBrowserSwitchResult_whenErrorNotNull_callsBackError() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        Exception expectedError = new Exception("error");
        SEPADirectDebitPaymentAuthResult
                payPalBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(expectedError);
        sut.tokenize(payPalBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Failure);
        assertEquals(expectedError, ((SEPADirectDebitResult.Failure) result).getError());
    }

    @Test
    public void onBrowserSwitchResult_whenResultAndErrorNull_callsBackUnexpectedError() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                payPalBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult((BrowserSwitchResult) null);
        sut.tokenize(payPalBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Failure);
        assertEquals("An unexpected error occurred.", ((SEPADirectDebitResult.Failure) result).getError().getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_callsBackUserCanceledException_andSendsAnalytics() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);
        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Cancel);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsSuccess_callsTokenize_andSendsAnalytics()
            throws JSONException {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        JSONObject metadata = new JSONObject()
                .put("ibanLastFour", "1234")
                .put("customerId", "customer-id")
                .put("bankReferenceToken", "bank-reference-token")
                .put("mandateType", "ONE_OFF");

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(
                Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.success");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"),
                eq("bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitInternalTokenizeCallback.class));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeSuccess_callsBackNonce_andSendsAnalytics()
            throws JSONException {
        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(
                new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .tokenizeSuccess(nonce)
                .build();

        JSONObject metadata = new JSONObject()
                .put("ibanLastFour", "1234")
                .put("customerId", "customer-id")
                .put("bankReferenceToken", "bank-reference-token")
                .put("mandateType", "ONE_OFF");

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(
                Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Success);
        assertEquals(nonce, ((SEPADirectDebitResult.Success) result).getNonce());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeFailure_callsBackError_andSendsAnalytics()
            throws JSONException {
        Exception exception = new Exception("tokenize error");
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .tokenizeError(exception)
                .build();

        JSONObject metadata = new JSONObject()
                .put("ibanLastFour", "1234")
                .put("customerId", "customer-id")
                .put("bankReferenceToken", "bank-reference-token")
                .put("mandateType", "ONE_OFF");

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(
                Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Failure);
        assertEquals(exception, ((SEPADirectDebitResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"),
                eq("bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitInternalTokenizeCallback.class));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeSuccess_callsBackResult()
            throws JSONException {
        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(
                new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .tokenizeSuccess(nonce)
                .build();

        JSONObject metadata = new JSONObject()
                .put("ibanLastFour", "1234")
                .put("customerId", "customer-id")
                .put("bankReferenceToken", "bank-reference-token")
                .put("mandateType", "ONE_OFF");

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(
                Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Success);
        assertEquals(nonce, ((SEPADirectDebitResult.Success) result).getNonce());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsCancel_returnsErrorToListener_andSendsAnalytics() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(
                "com.braintreepayments.demo.braintree://sepa/cancel?error_code=internal_error"));

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Failure);
        assertTrue(((SEPADirectDebitResult.Failure) result).getError() instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", ((SEPADirectDebitResult.Failure) result).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkURLIsNull_returnsErrorToListener() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(null);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                sepaBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult(browserSwitchResult);

        sut.tokenize(sepaBrowserSwitchResult, sepaTokenizeCallback);

        ArgumentCaptor<SEPADirectDebitResult> captor = ArgumentCaptor.forClass(SEPADirectDebitResult.class);
        verify(sepaTokenizeCallback).onSEPADirectDebitResult(captor.capture());

        SEPADirectDebitResult result = captor.getValue();
        assertTrue(result instanceof SEPADirectDebitResult.Failure);
        Exception exception = ((SEPADirectDebitResult.Failure) result).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Unknown error", exception.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");
    }
}
