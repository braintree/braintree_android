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
public class SEPADebitClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private BraintreeClient braintreeClient;
    private SEPADebitListener listener;
    private CreateMandateResult createMandateResult;
    private SEPADebitRequest sepaDebitRequest;

    @Before
    public void beforeEach() {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        braintreeClient = new MockBraintreeClientBuilder()
                .returnUrlScheme("com.example")
                .configuration(mock(Configuration.class))
                .build();
        listener = mock(SEPADebitListener.class);

        createMandateResult = new CreateMandateResult(
                "http://www.example.com",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );
        sepaDebitRequest = new SEPADebitRequest();
    }


    @Test
    public void constructor_setsLifecycleObserver() {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);

        ArgumentCaptor<SEPADebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDebitClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        SEPADebitClient sut = new SEPADebitClient(fragment, braintreeClient);

        ArgumentCaptor<SEPADebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDebitClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

        SEPADebitClient sut = new SEPADebitClient(activity, braintreeClient);

        ArgumentCaptor<SEPADebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADebitLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        SEPADebitLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.sepaDebitClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        SEPADebitClient sut = new SEPADebitClient(null, null, braintreeClient, sepaDebitApi);

        ArgumentCaptor<SEPADebitLifecycleObserver> captor = ArgumentCaptor.forClass(SEPADebitLifecycleObserver.class);
        verify(lifecycle, never()).addObserver(captor.capture());
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_launchesBrowserSwitch() throws BrowserSwitchException, JSONException {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());
        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(Uri.parse("http://www.example.com"), browserSwitchOptions.getUrl());
        assertEquals("com.example", browserSwitchOptions.getReturnUrlScheme());
        assertEquals(BraintreeRequestCodes.SEPA, browserSwitchOptions.getRequestCode());
        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("1234", metadata.get("ibanLastFour"));
        assertEquals("fake-customer-id", metadata.get("customerId"));
        assertEquals("fake-bank-reference-token", metadata.get("bankReferenceToken"));
        assertEquals("ONE_OFF", metadata.get("mandateType"));
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

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", exception.getMessage());
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenApprovalURLNull_callsSEPADebitAPI_tokenize() throws BrowserSwitchException {
        createMandateResult = new CreateMandateResult(
                "null",
                "1234",
                "fake-customer-id",
                "fake-bank-reference-token",
                "ONE_OFF"
        );

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);
        verify(braintreeClient, never()).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));
        verify(sepaDebitApi).tokenize("1234", "fake-customer-id", "fake-bank-reference-token", "ONE_OFF");
    }

    @Test
    public void tokenize_onCreateMandateRequestSuccess_whenStartBrowserSwitchFails_returnsErrorToListener() throws BrowserSwitchException {
        doThrow(BrowserSwitchException.class).when(braintreeClient).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateResultSuccess(createMandateResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BrowserSwitchException);
    }

    @Test
    public void tokenize_onConfigurationError_returnsErrorToListener() {
        Exception error = new Exception("config error");
        braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(error)
                .build();

        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        verify(listener).onSEPADebitFailure(same(error));
    }

    @Test
    public void tokenize_onCreateMandateError_returnsErrorToListener() {
        Exception error = new Exception("error");
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder()
                .createMandateError(error)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.tokenize(activity, sepaDebitRequest);

        verify(listener).onSEPADebitFailure(same(error));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusCanceled_returnsUserCanceledExceptionToListener() {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);
        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled SEPA Debit.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsSuccess_callsTokenize() throws JSONException {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

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

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(sepaDebitApi).tokenize("1234", "customer-id", "bank-reference-token", "ONE_OFF");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkContainsCancel_returnsErrorToListener() {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse("com.braintreepayments.demo.braintree://sepa/cancel?error_code=internal_error"));

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchStatusSuccess_whenDeepLinkURLIsNull_returnsErrorToListener() {
        SEPADebitApi sepaDebitApi = new MockSepaDebitApiBuilder().build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(null);

        braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        SEPADebitClient sut = new SEPADebitClient(activity, lifecycle, braintreeClient, sepaDebitApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onSEPADebitFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("Unknown error", exception.getMessage());
    }
}