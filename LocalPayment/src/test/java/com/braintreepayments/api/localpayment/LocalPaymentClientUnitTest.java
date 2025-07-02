package com.braintreepayments.api.localpayment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BrowserSwitchFinalResult;
import com.braintreepayments.api.BrowserSwitchOptions;
import com.braintreepayments.api.LaunchType;
import com.braintreepayments.api.core.AnalyticsEventParams;
import com.braintreepayments.api.core.AnalyticsParamRepository;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.ConfigurationException;
import com.braintreepayments.api.core.PostalAddress;
import com.braintreepayments.api.datacollector.DataCollector;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;

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
    private AnalyticsParamRepository analyticsParamRepository;

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
        analyticsParamRepository = mock(AnalyticsParamRepository.class);
        localPaymentAuthRequestParams = mock(LocalPaymentAuthRequestParams.class);
        when(localPaymentAuthRequestParams.getApprovalUrl()).thenReturn("https://");
        when(localPaymentAuthRequestParams.getRequest()).thenReturn(getIdealLocalPaymentRequest());
        when(localPaymentAuthRequestParams.getPaymentId()).thenReturn("paymentId");
        when(analyticsParamRepository.getSessionId()).thenReturn("sample-session-id");

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void createPaymentAuthRequest_resetsSessionId() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, dataCollector, localPaymentApi, analyticsParamRepository);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(analyticsParamRepository).reset();
    }

    @Test
    public void createPaymentAuthRequest_sendsPaymentStartedEvent() {
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector, localPaymentApi, analyticsParamRepository);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED, new AnalyticsEventParams(), true);
    }

    @Test
    public void createPaymentAuthRequest_sendsPaymentFailedEvent_forNullGetPaymentType() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        LocalPaymentClient sut = new LocalPaymentClient(
            braintreeClient,
            dataCollector,
            localPaymentApi,
            analyticsParamRepository
        );
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        String errorDescription = "LocalPaymentRequest is invalid, paymentType and amount are required.";
        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            new AnalyticsEventParams(null, false, null, null, null, null, null, null, null, null, null, errorDescription),
            true
        );
    }

    @Test
    public void createPaymentAuthRequest_createsPaymentMethodWithLocalPaymentApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);
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
                localPaymentApi, analyticsParamRepository);
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
                localPaymentApi, analyticsParamRepository);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            new AnalyticsEventParams("paymentId"),
            true
        );
    }

    @Test
    public void createPaymentAuthRequest_configurationFetchError_forwardsErrorToCallback() {
        Exception configException = new Exception(("Configuration not fetched"));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(configException)
            .build();

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);
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
                localPaymentApi, analyticsParamRepository);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        String errorDescription = "An error occurred creating the local payment method.";
        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            new AnalyticsEventParams(null, false, null, null, null, null, null, null, null, null, null, errorDescription),
            true
        );
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalDisabled_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalDisabledConfig)
            .build();

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);
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
                localPaymentApi, analyticsParamRepository);
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
                localPaymentApi, analyticsParamRepository);
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
    public void createPaymentAuthRequest_whenCreatePaymentMethodError_returnsErrorToCallback() {
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
            .createPaymentMethodError(new Exception("error"))
            .build();
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

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
                localPaymentApi, analyticsParamRepository);

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
            transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "");

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
            .createPaymentMethodSuccess(transaction)
            .build();

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED, new AnalyticsEventParams(), true);
        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            new AnalyticsEventParams(),
            true
        );
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
                localPaymentApi, analyticsParamRepository);
        sut.createPaymentAuthRequest(getIdealLocalPaymentRequest(), localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.PAYMENT_STARTED, new AnalyticsEventParams(), true);
        AnalyticsEventParams params = new AnalyticsEventParams(
            "some-paypal-context-id"
        );
        verify(braintreeClient).sendAnalyticsEvent(LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED, params, true);
    }

    @Test
    public void buildBrowserSwitchOptions_returnsLocalPaymentResultWithBrowserSwitchOptions()
        throws JSONException {
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
            transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT.getCode(), browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"),
            browserSwitchOptions.getUrl());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        JSONObject expectedMetadata = new JSONObject()
            .put("merchant-account-id", "local-merchant-account-id")
            .put("payment-type", "ideal")
            .put("has-user-location-consent", true);

        JSONAssert.assertEquals(expectedMetadata, metadata, true);
    }

    @Test
    public void buildBrowserSwitchOptions_startsBrowserSwitchWithClearTop() {
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(true);
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
            transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback);

        ArgumentCaptor<LocalPaymentAuthRequest> captor = ArgumentCaptor.forClass(LocalPaymentAuthRequest.class);
        verify(localPaymentAuthCallback).onLocalPaymentAuthRequest(captor.capture());

        LocalPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof LocalPaymentAuthRequest.ReadyToLaunch);
        LocalPaymentAuthRequestParams params = ((LocalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();

        assert browserSwitchOptions != null;
        assertEquals(LaunchType.ACTIVITY_CLEAR_TOP, browserSwitchOptions.getLaunchType());
    }

    @Test
    public void buildBrowserSwitchOptions_sendsAnalyticsEvents() {
        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentAuthRequestParams
            transaction = new LocalPaymentAuthRequestParams(request, approvalUrl, "payment-id");

        sut.buildBrowserSwitchOptions(transaction, true, localPaymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED,
            new AnalyticsEventParams(),
            true
        );
    }

    @Test
    public void tokenize_whenPostFailure_notifiesCallbackOfErrorAlongWithAnalyticsEvent()
        throws JSONException {
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);
        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
            browserSwitchResult);

        Exception postError = new Exception("POST failed");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .sendPOSTErrorResponse(postError)
            .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
            .tokenizeError(postError)
            .build();

        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn(
            "sample-correlation-id");

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
            captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Failure);
        Exception exception = ((LocalPaymentResult.Failure) result).getError();
        assertEquals(postError, exception);

        AnalyticsEventParams params = new AnalyticsEventParams(null, false, null, null, null, null, null, null, null, null, null, "POST failed");
        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_FAILED,
            params,
            true
        );
    }

    @Test
    public void tokenize_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi()
        throws JSONException {
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
            browserSwitchResult);

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, false)).thenReturn(
            "sample-correlation-id");

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        verify(localPaymentApi).tokenize(eq("local-merchant-account-id"), eq(webUrl),
            eq("sample-correlation-id"), any(LocalPaymentInternalTokenizeCallback.class));
    }

    @Test
    public void tokenize_whenResultOKAndTokenizationSucceeds_sendsResultToCallback()
        throws JSONException {
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
            browserSwitchResult);

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
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
                localPaymentApi, analyticsParamRepository);

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
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
           browserSwitchResult);
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
                localPaymentApi, analyticsParamRepository);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_SUCCEEDED,
            new AnalyticsEventParams(),
            true
        );
    }

    @Test
    public void tokenize_whenResultOK_onConfigurationError_returnsError()
        throws JSONException {
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
            browserSwitchResult);

        Exception configError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(configError)
            .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig, true)).thenReturn(
            "sample-correlation-id");

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

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
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(
            browserSwitchResult);

        LocalPaymentClient sut =
            new LocalPaymentClient(braintreeClient, dataCollector,
                localPaymentApi, analyticsParamRepository);

        sut.tokenize(activity, localPaymentAuthResult, localPaymentTokenizeCallback);

        ArgumentCaptor<LocalPaymentResult> captor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentTokenizeCallback).onLocalPaymentResult(
            captor.capture());

        LocalPaymentResult result = captor.getValue();
        assertTrue(result instanceof LocalPaymentResult.Cancel);
        verify(braintreeClient).sendAnalyticsEvent(
            LocalPaymentAnalytics.PAYMENT_CANCELED,
            new AnalyticsEventParams(),
            true
        );
    }

    @Test
    public void onBrowserSwitchResult_sends_the_correct_value_of_hasUserLocationConsent_to_getClientMetadataId() throws JSONException {
        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
            .put("payment-type", "ideal")
            .put("merchant-account-id", "local-merchant-account-id")
            .put("has-user-location-consent", true)
        );

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getReturnUrl()).thenReturn(Uri.parse(webUrl));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(payPalEnabledConfig)
            .build();
        when(dataCollector.getClientMetadataId(any(Context.class), same(payPalEnabledConfig), anyBoolean())).thenReturn("client-metadata-id");

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, dataCollector, localPaymentApi, analyticsParamRepository);
        LocalPaymentAuthResult.Success localPaymentAuthResult = new LocalPaymentAuthResult.Success(browserSwitchResult);

        sut.createPaymentAuthRequest(request, localPaymentAuthCallback);

        sut.tokenize(activity, localPaymentAuthResult, mock());

        verify(dataCollector).getClientMetadataId(any(), same(payPalEnabledConfig), eq(true));
    }

    private LocalPaymentRequest getIdealLocalPaymentRequest() {
        PostalAddress address = new PostalAddress();
        address.setStreetAddress("836486 of 22321 Park Lake");
        address.setExtendedAddress("Apt 2");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Den Haag");
        address.setRegion("CA");
        address.setPostalCode("2585 GJ");

        return new LocalPaymentRequest(true, address, "1.10", "bank-id-code", "EUR", "My Brand!",
            "jon@getbraintree.com", "Jon", "local-merchant-account-id", "ideal", "NL", "639847934",
            true, "Doe");
    }
}
