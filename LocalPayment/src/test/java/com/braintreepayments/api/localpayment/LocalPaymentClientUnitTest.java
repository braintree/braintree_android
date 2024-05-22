package com.braintreepayments.api.localpayment;

import static com.ibm.icu.impl.Assert.fail;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.Fixtures;
import com.braintreepayments.api.MockBraintreeClientBuilder;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.ConfigurationException;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.datacollector.DataCollector;

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
    private LocalPaymentAuthCallback localPaymentAuthCallback;
    private LocalPaymentTokenizeCallback localPaymentTokenizeCallback;
    private BraintreeClient braintreeClient;
    private DataCollector dataCollector;
    private LocalPaymentApi localPaymentApi;
    private LocalPaymentAuthRequestParams localPaymentAuthRequestParams;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        localPaymentAuthCallback = mock(LocalPaymentAuthCallback.class);
        localPaymentTokenizeCallback = mock(LocalPaymentTokenizeCallback.class);

        braintreeClient =
                new MockBraintreeClientBuilder().configuration(
                        Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)).build();
        dataCollector = mock(DataCollector.class);
        localPaymentApi = mock(LocalPaymentApi.class);
        localPaymentAuthRequestParams = mock(LocalPaymentAuthRequestParams.class);
        when(localPaymentAuthRequestParams.getApprovalUrl()).thenReturn("https://");
        when(localPaymentAuthRequestParams.getRequest()).thenReturn(getIdealLocalPaymentRequest());

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void createPaymentAuthRequest_sendsPaymentStartedEvent() {
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector, localPaymentApi);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED);
    }

    @Test
    public void createPaymentAuthRequest_sendsPaymentFailedEvent_forNullRequest() {
        LocalPaymentClient sut = new LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi);
        sut.createPaymentAuthRequest(null, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, null, null);
    }

    @Test
    public void createPaymentAuthRequest_sendsPaymentFailedEvent_forNullGetPaymentType() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        LocalPaymentClient sut = new LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi);
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, null, null);
    }

    @Test
    public void createPaymentAuthRequest_createsPaymentMethodWithLocalPaymentApi() {
       BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        verify(localPaymentApi).createPaymentMethod(same(request),
                any(LocalPaymentInternalAuthRequestCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_success_forwardsResultToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentAuthRequestParams)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        assertEquals(localPaymentAuthRequestParams, params);
    }

    @Test
    public void createPaymentAuthRequest_success_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentAuthRequestParams)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, null, null);
    }

    @Test
    public void createPaymentAuthRequest_configurationFetchError_forwardsErrorToCallback() {
        Exception configException = new Exception(("Configuration not fetched"));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configException)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertEquals(exception, configException);
    }

    @Test
    public void createPaymentAuthRequest_onLocalPaymentApiError_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodError(new Exception("error"))
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, null, null);
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalDisabled_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("Local payments are not enabled for this merchant.", exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenAmountIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setAmount(null);

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.",
                exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenPaymentTypeIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.",
                exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenLocalPaymentRequestIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.createPaymentAuthRequest(null, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("A LocalPaymentRequest is required.", exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenCallbackIsNull_throwsError() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        try {
            sut.createPaymentAuthRequest(request, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentAuthRequestCallback is required.", exception.getMessage());
        }
    }

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentMethodError_returnsErrorToCallback() {
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodError(new Exception("error"))
                .build();
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(),
                localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.Failure);
        Exception exception = ((LocalPaymentAuthRequest.Failure) paymentAuthRequest).getError();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("An error occurred creating the local payment method.",
                exception.getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentMethodSuccess_returnsLocalPaymentResultToCallback() {
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentAuthRequestParams)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        assertEquals(localPaymentAuthRequestParams, params);
    }

    @Test
    public void createPaymentAuthRequest_success_withEmptyPaymentId_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
                transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, null);

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(transaction)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED);
        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, null, null);
    }


    @Test
    public void createPaymentAuthRequest_success_withPaymentId_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
                transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "some-paypal-context-id");

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(transaction)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED);
        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, "some-paypal-context-id", null);
    }

    @Test
    public void buildBrowserSwitchOptions_returnsLocalPaymentResultWithBrowserSwitchOptions()
            throws JSONException {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
                transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"),
                browserSwitchOptions.getUrl());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        JSONObject expectedMetadata = new JSONObject()
                .put("merchant-account-id", "local-merchant-account-id")
                .put("payment-type", "ideal");

        JSONAssert.assertEquals(expectedMetadata, metadata, true);
    }

    @Test
    public void buildBrowserSwitchOptions_withDefaultDeepLinkHandlerEnabled_startsBrowserSwitchAsNewTaskWithProperRequestCode() {
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(true);
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
                transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();

        assertTrue(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void buildBrowserSwitchOptions_sendsAnalyticsEvents() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
                transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, null, null);
    }

    @Test
    public void tokenize_whenResultOK_uriNull_notifiesCallbackOfErrorAlongWithAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
                captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Failure);
        Exception exception = ((LocalPaymentResult.Failure) result).getError();
        assertTrue(exception instanceof BraintreeException);

        String expectedMessage = "LocalPayment encountered an error, return URL is invalid.";
        assertEquals(expectedMessage, exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, null, null);
    }

    @Test
    public void tokenize_whenPostFailure_notifiesCallbackOfErrorAlongWithAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        Exception postError = new Exception("POST failed");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTErrorResponse(postError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeError(postError)
                .build();

        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
                captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Failure);
        Exception exception = ((LocalPaymentResult.Failure) result).getError();
        assertEquals(postError, exception);
        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_FAILED, null, null);
    }

    @Test
    public void tokenize_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        verify(localPaymentApi).tokenize(eq("local-merchant-account-id"), eq(webUrl),
                eq("sample-correlation-id"), any(LocalPaymentInternalTokenizeCallback.class));
    }

    @Test
    public void tokenize_whenResultOKAndTokenizationSucceeds_sendsResultToCallback()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .integration("custom")
                .sessionId("session-id")
                .build();
        when(dataCollector.getClientMetadataId(any(Context.class),
                same(payPalEnabledConfig), eq(false))).thenReturn("client-metadata-id");

        LocalPaymentNonce successNonce = LocalPaymentNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(successNonce)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
                captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Success);
        LocalPaymentNonce nonce = ((LocalPaymentResult.Success) result).getNonce();
        assertEquals(successNonce, nonce);
    }

    @Test
    public void tokenize_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(LocalPaymentNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)))
                .build();

        when(dataCollector.getClientMetadataId(any(Context.class),
                same(payPalEnabledConfig), eq(false))).thenReturn("client-metadata-id");


        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_SUCCEEDED, null, null);
    }

    @Test
    public void tokenize_whenResultOK_onConfigurationError_returnsError()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        Exception configError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, true)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
                captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Failure);
        Exception exception = ((LocalPaymentResult.Failure) result).getError();
        assertEquals(configError, exception);
    }

    @Test
    public void tokenize_whenResultOKAndUserCancels_notifiesCallbackAndSendsAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResultInfo browserSwitchResult = mock(BrowserSwitchResultInfo.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
                new LocalPaymentAuthResultInfo(browserSwitchResult));

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
                captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Cancel);
        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_CANCELED, null, null);
    }

    // TODO: Update test when hasUserLocationConsent passing is fixed
//    @Test
//    public void onBrowserSwitchResult_sends_the_correct_value_of_hasUserLocationConsent_to_getClientMetadataId() throws JSONException {
//        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
//        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);
//
//        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
//            .put("payment-type", "ideal")
//            .put("merchant-account-id", "local-merchant-account-id"));
//
//        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
//        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
//            .configuration(payPalEnabledConfig)
//            .integration("custom")
//            .sessionId("session-id")
//            .build();
//        when(payPalDataCollector.getClientMetadataId(any(Context.class), same(payPalEnabledConfig), anyBoolean())).thenReturn("client-metadata-id");
//
//        LocalPaymentNonce successNonce = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
//        LocalPaymentResult localPaymentResult = mock(LocalPaymentResult.class);
//        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
//            .tokenizeSuccess(successNonce)
//            .createPaymentMethodSuccess(localPaymentResult)
//            .build();
//
//        LocalPaymentRequest request = getIdealLocalPaymentRequest();
//        LocalPaymentClient sut = new LocalPaymentClient(activity, lifecycle, braintreeClient, payPalDataCollector, localPaymentApi);
//
//        sut.startPayment(request, localPaymentStartCallback);
//
//        sut.setListener(listener);
//        sut.onBrowserSwitchResult(activity, browserSwitchResult);
//
//        verify(payPalDataCollector).getClientMetadataId(any(), same(payPalEnabledConfig), eq(true));
//    }

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
