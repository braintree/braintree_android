package com.braintreepayments.api;

import static com.ibm.icu.impl.Assert.fail;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
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
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private LocalPaymentStartCallback localPaymentStartCallback;
    private LocalPaymentListener listener;

    private BraintreeClient braintreeClient;
    private PayPalDataCollector payPalDataCollector;
    private LocalPaymentApi localPaymentApi;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        localPaymentStartCallback = mock(LocalPaymentStartCallback.class);
        listener = mock(LocalPaymentListener.class);

        braintreeClient = mock(BraintreeClient.class);
        payPalDataCollector = mock(PayPalDataCollector.class);
        localPaymentApi = mock(LocalPaymentApi.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void constructor_setsLifecycleObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        ArgumentCaptor<LocalPaymentLifecycleObserver> captor = ArgumentCaptor.forClass(LocalPaymentLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        LocalPaymentLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.localPaymentClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        LocalPaymentClient sut = new LocalPaymentClient(fragment, braintreeClient);

        ArgumentCaptor<LocalPaymentLifecycleObserver> captor = ArgumentCaptor.forClass(LocalPaymentLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        LocalPaymentLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.localPaymentClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

        LocalPaymentClient sut = new LocalPaymentClient(activity, braintreeClient);

        ArgumentCaptor<LocalPaymentLifecycleObserver> captor = ArgumentCaptor.forClass(LocalPaymentLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        LocalPaymentLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.localPaymentClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut = new LocalPaymentClient(null, null, braintreeClient, payPalDataCollector, localPaymentApi);

        verify(lifecycle, never()).addObserver(any(LocalPaymentLifecycleObserver.class));
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultExists_deliversResultToListener_andSetsPendingResultNull() throws JSONException {
        LocalPaymentNonce nonce = mock(LocalPaymentNonce.class);
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(nonce)
                .build();

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(activity);
        when(payPalDataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.pendingBrowserSwitchResult = browserSwitchResult;
        sut.setListener(listener);

        verify(listener).onLocalPaymentSuccess(same(nonce));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultDoesNotExist_doesNotInvokeListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut = new LocalPaymentClient(null, null, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.pendingBrowserSwitchResult = null;
        sut.setListener(listener);

        verify(listener, never()).onLocalPaymentSuccess(any(LocalPaymentNonce.class));
        verify(listener, never()).onLocalPaymentFailure(any(Exception.class));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void startPayment_createsPaymentMethodWithLocalPaymentApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentApi).createPaymentMethod(same(request), any(LocalPaymentStartCallback.class));
    }

    @Test
    public void startPayment_success_forwardsResultToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentResult localPaymentResult = mock(LocalPaymentResult.class);
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentResult)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(same(localPaymentResult), (Exception) isNull());
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(mock(LocalPaymentResult.class))
                .build();
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.start-payment.selected"),
                payloadCaptor.capture()
        );
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.create.succeeded"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void startPayment_success_withEmptyPaymentId_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentResult localPaymentResult = mock(LocalPaymentResult.class);
        when(localPaymentResult.getPaymentId()).thenReturn("");

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentResult)
                .build();
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.start-payment.selected"),
                payloadCaptor.capture()
        );
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.create.succeeded"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void startPayment_success__withPaymentId_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentResult localPaymentResult = mock(LocalPaymentResult.class);
        when(localPaymentResult.getPaymentId()).thenReturn("some-paypal-context-id");

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentResult)
                .build();
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        expectedPayload.setPayPalContextId("some-paypal-context-id");
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.start-payment.selected"),
                payloadCaptor.capture()
        );
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.create.succeeded"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }
    @Test
    public void startPayment_configurationFetchError_forwardsErrorToCallback() {
        Exception configException = new Exception(("Configuration not fetched"));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configException)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), same(configException));
    }

    @Test
    public void startPayment_onLocalPaymentApiError_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodError(new Exception("error"))
                .build();
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.start-payment.selected"),
                payloadCaptor.capture()
        );
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.webswitch.initiate.failed"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void startPayment_onConfigurationFetchError_forwardsErrorToCallback() {
        Exception configError = new Exception("config fetch error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(null, configError);
    }

    @Test
    public void startPayment_whenPayPalDisabled_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("Local payments are not enabled for this merchant.", exception.getMessage());
    }

    @Test
    public void startPayment_whenAmountIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setAmount(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_whenPaymentTypeIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_whenLocalPaymentRequestIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        sut.startPayment(null, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("A LocalPaymentRequest is required.", exception.getMessage());
    }

    @Test
    public void startPayment_whenCallbackIsNull_throwsError() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        try {
            sut.startPayment(request, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentCallback is required.", exception.getMessage());
        }
    }

    @Test
    public void approvePayment_startsBrowserWithProperRequestCode() throws JSONException, BrowserSwitchException {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approveLocalPayment(activity, transaction);

        ArgumentCaptor<BrowserSwitchOptions> optionsCaptor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), optionsCaptor.capture());

        BrowserSwitchOptions browserSwitchOptions = optionsCaptor.getValue();
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"), browserSwitchOptions.getUrl());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        JSONObject expectedMetadata = new JSONObject()
                .put("merchant-account-id", "local-merchant-account-id")
                .put("payment-type", "ideal");

        JSONAssert.assertEquals(expectedMetadata, metadata, true);
    }

    @Test
    public void approvePayment_withDefaultDeepLinkHandlerEnabled_startsBrowserSwitchAsNewTaskWithProperRequestCode() throws JSONException, BrowserSwitchException {
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(true);
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approveLocalPayment(activity, transaction);

        ArgumentCaptor<BrowserSwitchOptions> optionsCaptor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), optionsCaptor.capture());

        BrowserSwitchOptions browserSwitchOptions = optionsCaptor.getValue();
        assertTrue(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void approvePayment_sendsAnalyticsEvents() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        sut.approveLocalPayment(activity, transaction);
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.webswitch.initiate.succeeded"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void approvePayment_whenActivityIsNull_returnsErrorToListener() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approveLocalPayment(null, transaction);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onLocalPaymentFailure(captor.capture());
        assertEquals("A FragmentActivity is required.", captor.getValue().getMessage());
    }

    @Test
    public void approvePayment_whenTransactionIsNull_returnsErrorToListener() {
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.approveLocalPayment(activity, null);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onLocalPaymentFailure(captor.capture());
        assertEquals("A LocalPaymentTransaction is required.", captor.getValue().getMessage());
    }

    @Test
    public void approvePayment_onBrowserSwitchStartError_returnsErrorToListener() throws BrowserSwitchException {
        BrowserSwitchException browserSwitchError = new BrowserSwitchException("error");
        doThrow(browserSwitchError).when(braintreeClient).startBrowserSwitch(any(FragmentActivity.class), any(BrowserSwitchOptions.class));
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approveLocalPayment(activity, transaction);
        verify(listener).onLocalPaymentFailure(same(browserSwitchError));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_uriNull_notifiesListenerOfErrorAlongWithAnalyticsEvent() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onLocalPaymentFailure(exceptionCaptor.capture());

        Exception exception = exceptionCaptor.getValue();
        assertTrue(exception instanceof BraintreeException);

        String expectedMessage = "LocalPayment encountered an error, return URL is invalid.";
        assertEquals(expectedMessage, exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.webswitch-response.invalid"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void onBrowserSwitchResult_whenPostFailure_notifiesListenerOfErrorAlongWithAnalyticsEvent() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        Exception postError = new Exception("POST failed");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTErrorResponse(postError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeError(postError)
                .build();

        when(payPalDataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        verify(listener).onLocalPaymentFailure(same(postError));
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.tokenize.failed"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(payPalDataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        verify(localPaymentApi).tokenize(eq("local-merchant-account-id"), eq(webUrl), eq("sample-correlation-id"), any(LocalPaymentBrowserSwitchResultCallback.class));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSucceeds_sendsResultToListener() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .integration("custom")
                .sessionId("session-id")
                .build();
        when(payPalDataCollector.getClientMetadataId(any(Context.class), same(payPalEnabledConfig), anyBoolean())).thenReturn("client-metadata-id");

        LocalPaymentNonce successNonce = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(successNonce)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);
        sut.setListener(listener);

        verify(listener).onLocalPaymentSuccess(same(successNonce));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)))
                .build();

        when(payPalDataCollector.getClientMetadataId(any(Context.class), same(payPalEnabledConfig), anyBoolean())).thenReturn("client-metadata-id");

        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.tokenize.succeeded"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_onConfigurationError_returnsError() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        Exception configError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(payPalDataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);
        verify(listener).onLocalPaymentFailure(configError);
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndUserCancels_notifiesListenerAndSendsAnalyticsEvent() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onLocalPaymentFailure(exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.webswitch.canceled"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void onBrowserSwitchResult_whenResultCANCELED_sendsAnalyticsEvent() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        AnalyticsEventParams expectedPayload = new AnalyticsEventParams();
        ArgumentCaptor<AnalyticsEventParams> payloadCaptor = ArgumentCaptor.forClass(AnalyticsEventParams.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onLocalPaymentFailure(exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent(
                eq("ideal.local-payment.webswitch.canceled"),
                payloadCaptor.capture()
        );
        assertEquals(expectedPayload, payloadCaptor.getValue());
    }

    @Test
    public void getBrowserSwitchResult_forwardsInvocationToBraintreeClient() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        assertSame(browserSwitchResult, sut.getBrowserSwitchResult(activity));
    }

    @Test
    public void deliverBrowserSwitchResult_forwardsInvocationToBraintreeClient() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        assertSame(browserSwitchResult, sut.deliverBrowserSwitchResult(activity));
    }

    @Test
    public void getBrowserSwitchResultFromCache_forwardsInvocationToBraintreeClient() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        assertSame(browserSwitchResult, sut.getBrowserSwitchResultFromNewTask(activity));
    }

    @Test
    public void deliverBrowserSwitchResultFromNewTask_forwardsInvocationToBraintreeClient() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(braintreeClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
        assertSame(browserSwitchResult, sut.deliverBrowserSwitchResultFromNewTask(activity));
    }

    @Test
    public void parseBrowserSwitchResult_forwardsInvocationToBraintreeClient() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);

        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        Intent intent = new Intent();
        when(
                braintreeClient.parseBrowserSwitchResult(activity, BraintreeRequestCodes.LOCAL_PAYMENT, intent)
        ).thenReturn(browserSwitchResult);

        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult result = sut.parseBrowserSwitchResult(activity, intent);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void clearActiveBrowserSwitchRequests_forwardsInvocationToBraintreeClient() {
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        sut.clearActiveBrowserSwitchRequests(activity);
        verify(braintreeClient).clearActiveBrowserSwitchRequests(activity);
    }

    @Test
    public void onBrowserSwitchResult_sends_the_correct_value_of_hasUserLocationConsent_to_getClientMetadataId() throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .integration("custom")
            .sessionId("session-id")
            .build();
        when(payPalDataCollector.getClientMetadataId(any(Context.class), same(payPalEnabledConfig), anyBoolean())).thenReturn("client-metadata-id");

        LocalPaymentNonce successNonce = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
        LocalPaymentResult localPaymentResult = mock(LocalPaymentResult.class);
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
            .tokenizeSuccess(successNonce)
            .createPaymentMethodSuccess(localPaymentResult)
            .build();

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);

        sut.startPayment(request, localPaymentStartCallback);

        sut.setListener(listener);
        sut.onBrowserSwitchResult(activity, browserSwitchResult);

        verify(payPalDataCollector).getClientMetadataId(any(), same(payPalEnabledConfig), eq(true));
    }

    private LocalPaymentRequest getIdealLocalPaymentRequest() {
        PostalAddress address = new PostalAddress();
        address.setStreetAddress("836486 of 22321 Park Lake");
        address.setExtendedAddress("Apt 2");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Den Haag");
        address.setRegion("CA");
        address.setPostalCode("2585 GJ");

        LocalPaymentRequest request = new LocalPaymentRequest(true);
        request.setPaymentType("ideal");
        request.setAmount("1.10");
        request.setAddress(address);
        request.setPhone("639847934");
        request.setEmail("jon@getbraintree.com");
        request.setGivenName("Jon");
        request.setSurname("Doe");
        request.setShippingAddressRequired(false);
        request.setMerchantAccountId("local-merchant-account-id");
        request.setCurrencyCode("EUR");
        request.setPaymentTypeCountryCode("NL");
        request.setDisplayName("My Brand!");

        return request;
    }
}