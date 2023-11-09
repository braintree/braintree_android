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
    private SEPADirectDebitBrowserSwitchResultCallback sepaBrowserSwitchResultCallback;
    private SEPADirectDebitPaymentAuthRequestCallback sepaFlowStartedCallback;

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

        sepaBrowserSwitchResultCallback = mock(SEPADirectDebitBrowserSwitchResultCallback.class);
        sepaFlowStartedCallback = mock(SEPADirectDebitPaymentAuthRequestCallback.class);
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_callsBackSEPAResponse_andSendsAnalytics()
            throws JSONException {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.selected.started");

        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthRequest.class);
        verify(sepaFlowStartedCallback).onResult(captor.capture(), isNull());

        SEPADirectDebitPaymentAuthRequest sepaResponseResult = captor.getValue();

        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.requested");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");

        BrowserSwitchOptions browserSwitchOptions = sepaResponseResult.getBrowserSwitchOptions();
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

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);

        ArgumentCaptor<SEPADirectDebitPaymentAuthRequest> captor = ArgumentCaptor.forClass(
                SEPADirectDebitPaymentAuthRequest.class);
        verify(sepaFlowStartedCallback).onResult(captor.capture(), isNull());
        assertEquals(captor.getValue().getNonce(), nonce);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenMandateAlreadyApproved_onTokenizeFailure_callsBackError_andSendsAnalytics() {
        // null approval URL indicates mandate approved
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        Exception exception = new Exception("tokenize error");
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .tokenizeError(exception)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);
        verify(sepaFlowStartedCallback).onResult(isNull(), eq(exception));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLInvalid_callsBackError() {
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

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaFlowStartedCallback).onResult(isNull(), captor.capture());
        assertTrue(captor.getValue() instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", captor.getValue().getMessage());
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

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);
        verify(braintreeClient, never()).startBrowserSwitch(any(FragmentActivity.class),
                any(BrowserSwitchOptions.class));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("fake-customer-id"),
                eq("fake-bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitTokenizeCallback.class));
    }

    @Test
    public void tokenize_onCreateMandateError_returnsErrorToListener_andSendsAnalytics() {
        Exception error = new Exception("error");
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateError(error)
                .build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        sut.createPaymentAuthRequest(sepaDirectDebitRequest, sepaFlowStartedCallback);
        verify(sepaFlowStartedCallback).onResult(isNull(), eq(error));
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
        sut.tokenize(payPalBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), captor.capture());

        assertNotNull(captor.getValue());
        assertEquals(expectedError, captor.getValue());
    }

    @Test
    public void onBrowserSwitchResult_whenResultAndErrorNull_callsBackUnexpectedError() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADirectDebitClient sut =
                new SEPADirectDebitClient(braintreeClient, sepaDirectDebitApi);

        SEPADirectDebitPaymentAuthResult
                payPalBrowserSwitchResult = new SEPADirectDebitPaymentAuthResult((BrowserSwitchResult) null);
        sut.tokenize(payPalBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), captor.capture());

        assertNotNull(captor.getValue());
        assertEquals("An unexpected error occurred.", captor.getValue().getMessage());
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled SEPA Debit.", exception.getMessage());
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.success");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"),
                eq("bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitTokenizeCallback.class));
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        verify(sepaBrowserSwitchResultCallback).onResult(eq(nonce), isNull());
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), eq(exception));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"),
                eq("bank-reference-token"), eq("ONE_OFF"),
                any(SEPADirectDebitTokenizeCallback.class));
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        verify(sepaBrowserSwitchResultCallback).onResult(eq(nonce), isNull());
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", exception.getMessage());
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

        sut.tokenize(sepaBrowserSwitchResult, sepaBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(sepaBrowserSwitchResultCallback).onResult(isNull(), captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Unknown error", exception.getMessage());
    }
}
