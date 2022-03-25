package com.braintreepayments.api;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADirectDebitClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private BraintreeClient braintreeClient;
    private SEPADirectDebitListener listener;
    private CreateMandateResult createMandateResult;
    private SEPADirectDebitRequest sepaDirectDebitRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("com.example")
                .build();
        listener = mock(SEPADirectDebitListener.class);

        createMandateResult = new CreateMandateResult(
                "http://www.example.com",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );
        sepaDirectDebitRequest = new SEPADirectDebitRequest();
    }

    @Test
    public void constructor_setsLifecycleObserver() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);

        ArgumentCaptor<SEPADirectDebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADirectDebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADirectDebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDirectDebitClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        SEPADirectDebitClient sut = new SEPADirectDebitClient(fragment, braintreeClient);

        ArgumentCaptor<SEPADirectDebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADirectDebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADirectDebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDirectDebitClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, braintreeClient);

        ArgumentCaptor<SEPADirectDebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADirectDebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADirectDebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDirectDebitClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(null, null, braintreeClient, sepaDirectDebitApi);

        ArgumentCaptor<SEPADirectDebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADirectDebitLifecycleObserver.class);
        verify(lifecycle, never()).addObserver(captor.capture());
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_launchesBrowserSwitch_andSendsAnalytics() throws BrowserSwitchException, JSONException {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);

        sut.tokenize(activity, sepaDirectDebitRequest);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.selected.started");

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.requested");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
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
    public void tokenize_onCreateMandateRequestSuccess_whenMandateAlreadyApproved_onTokenizeSuccess_forwardsResultToListener_andSendsAnalytics() throws JSONException {
        // null approval URL indicates mandate approved
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .tokenizeSuccess(nonce)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);
        verify(listener).onSEPADirectDebitSuccess(nonce);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenMandateAlreadyApproved_onTokenizeFailure_forwardsErrorToListener_andSendsAnalytics() {
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

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);
        verify(listener).onSEPADirectDebitFailure(exception);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLInvalid_returnsErrorToListener() {
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

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADirectDebitFailure(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", exception.getMessage());
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLNull_callsSEPADirectDebitAPI_tokenize_andSendsAnalytics() throws BrowserSwitchException {
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

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);
        verify(braintreeClient, never()).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.success");
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("fake-customer-id"), eq("fake-bank-reference-token"), eq("ONE_OFF"), any(SEPADirectDebitTokenizeCallback.class));
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenStartBrowserSwitchFails_returnsErrorToListener_andSendsAnalytics() throws BrowserSwitchException {
        doThrow(BrowserSwitchException.class).when(braintreeClient).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));

        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADirectDebitFailure(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BrowserSwitchException);
    }

    @Test
    public void tokenize_onCreateMandateError_returnsErrorToListener_andSendsAnalytics() {
        Exception error = new Exception("error");
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder()
                .createMandateError(error)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDirectDebitRequest);

        verify(listener).onSEPADirectDebitFailure(same(error));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.create-mandate.failure");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_returnsUserCanceledExceptionToListener_andSendsAnalytics() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);
        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADirectDebitFailure(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.canceled");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled SEPA Debit.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsSuccess_callsTokenize_andSendsAnalytics() throws JSONException {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        JSONObject metadata = new JSONObject()
                .put("ibanLastFour", "1234")
                .put("customerId", "customer-id")
                .put("bankReferenceToken", "bank-reference-token")
                .put("mandateType", "ONE_OFF");

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.success");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"), eq("bank-reference-token"), eq("ONE_OFF"), any(SEPADirectDebitTokenizeCallback.class));
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.requested");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeSuccess_forwardsResultToListener_andSendsAnalytics() throws JSONException {
        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
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
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(listener).onSEPADirectDebitSuccess(nonce);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.success");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeFailure_forwardsErrorToListener_andSendsAnalytics() throws JSONException {
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
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(listener).onSEPADirectDebitFailure(exception);
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.tokenize.failure");
        verify(sepaDirectDebitApi).tokenize(eq("1234"), eq("customer-id"), eq("bank-reference-token"), eq("ONE_OFF"), any(SEPADirectDebitTokenizeCallback.class));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeSuccess_forwardsResultToListener() throws JSONException {
        SEPADirectDebitNonce nonce = SEPADirectDebitNonce.fromJSON(new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
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
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(listener).onSEPADirectDebitSuccess(nonce);
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_onTokenizeFailure_forwardsErrorToListener() throws JSONException {
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
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/success?success=true"));
        when(browserSwitchResult.getRequestMetadata()).thenReturn(metadata);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(listener).onSEPADirectDebitFailure(exception);
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsCancel_returnsErrorToListener_andSendsAnalytics() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/cancel?error_code=internal_error"));

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADirectDebitFailure(captor.capture());
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

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADirectDebitFailure(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent("sepa-direct-debit.browser-switch.failure");

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Unknown error", exception.getMessage());
    }

    @Test
    public void getBrowserSwitchResult_getBrowserSwitchResultFromBraintreeClient() {
        SEPADirectDebitApi sepaDirectDebitApi = new MockSEPADirectDebitApiBuilder().build();

        braintreeClient = new MockBraintreeClientBuilder()
                .build();

        SEPADirectDebitClient sut = new SEPADirectDebitClient(activity, lifecycle, braintreeClient, sepaDirectDebitApi);
        sut.getBrowserSwitchResult(activity);
        verify(braintreeClient).getBrowserSwitchResult(activity);
    }
}