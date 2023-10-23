package com.braintreepayments.api;

import static com.ibm.icu.impl.Assert.fail;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.mockito.ArgumentMatchers.any;
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
    private LocalPaymentStartCallback localPaymentStartCallback;
    private LocalPaymentBrowserSwitchResultCallback localPaymentBrowserSwitchResultCallback;
    private BraintreeClient braintreeClient;
    private DataCollector dataCollector;
    private LocalPaymentApi localPaymentApi;
    private LocalPaymentResult localPaymentResult;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        localPaymentStartCallback = mock(LocalPaymentStartCallback.class);
        localPaymentBrowserSwitchResultCallback =
                mock(LocalPaymentBrowserSwitchResultCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        dataCollector = mock(DataCollector.class);
        localPaymentApi = mock(LocalPaymentApi.class);
        localPaymentResult = mock(LocalPaymentResult.class);
        when(localPaymentResult.getApprovalUrl()).thenReturn("https://");
        when(localPaymentResult.getRequest()).thenReturn(getIdealLocalPaymentRequest());

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void startPayment_createsPaymentMethodWithLocalPaymentApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder().build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentApi).createPaymentMethod(same(request),
                any(LocalPaymentStartCallback.class));
    }

    @Test
    public void startPayment_success_forwardsResultToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentResult)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(same(localPaymentResult), isNull());
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(localPaymentResult)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.start-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.create.succeeded");
    }

    @Test
    public void startPayment_configurationFetchError_forwardsErrorToCallback() {
        Exception configException = new Exception(("Configuration not fetched"));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configException)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(isNull(), same(configException));
    }

    @Test
    public void startPayment_onLocalPaymentApiError_sendsAnalyticsEvents() {
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
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.start-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.initiate.failed");
    }

    @Test
    public void startPayment_onConfigurationFetchError_forwardsErrorToCallback() {
        Exception configError = new Exception("config fetch error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(null, configError);
    }

    @Test
    public void startPayment_whenPayPalDisabled_returnsErrorToCallback() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
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
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setAmount(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.",
                exception.getMessage());
    }

    @Test
    public void startPayment_whenPaymentTypeIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.",
                exception.getMessage());
    }

    @Test
    public void startPayment_whenLocalPaymentRequestIsNull_returnsErrorToCallback() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

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
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        try {
            sut.startPayment(request, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentCallback is required.", exception.getMessage());
        }
    }

    @Test
    public void approvePayment_returnsLocalPaymentResultWithBrowserSwitchOptions()
            throws JSONException {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approvePayment(transaction, localPaymentStartCallback);

        ArgumentCaptor<LocalPaymentResult> captor =
                ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentStartCallback).onResult(captor.capture(), isNull());

        LocalPaymentResult result = captor.getValue();
        BrowserSwitchOptions browserSwitchOptions = result.getBrowserSwitchOptions();
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
    public void approvePayment_withDefaultDeepLinkHandlerEnabled_startsBrowserSwitchAsNewTaskWithProperRequestCode() {
        when(braintreeClient.launchesBrowserSwitchAsNewTask()).thenReturn(true);
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approvePayment(transaction, localPaymentStartCallback);

        ArgumentCaptor<LocalPaymentResult> captor =
                ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentStartCallback).onResult(captor.capture(), isNull());

        LocalPaymentResult result = captor.getValue();
        BrowserSwitchOptions browserSwitchOptions = result.getBrowserSwitchOptions();

        assertTrue(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void approvePayment_sendsAnalyticsEvents() {
        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approvePayment(transaction, localPaymentStartCallback);
        verify(braintreeClient).sendAnalyticsEvent(
                "ideal.local-payment.webswitch.initiate.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_uriNull_notifiesCallbackOfErrorAlongWithAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult(isNull(),
                exceptionCaptor.capture());

        Exception exception = exceptionCaptor.getValue();
        assertTrue(exception instanceof BraintreeException);

        String expectedMessage = "LocalPayment encountered an error, return URL is invalid.";
        assertEquals(expectedMessage, exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(
                "ideal.local-payment.webswitch-response.invalid");
    }

    @Test
    public void onBrowserSwitchResult_whenPostFailure_notifiesListenerOfErrorAlongWithAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

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

        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        verify(localPaymentBrowserSwitchResultCallback).onResult(isNull(), same(postError));
        verify(braintreeClient).sendAnalyticsEvent(eq("ideal.local-payment.tokenize.failed"));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        verify(localPaymentApi).tokenize(eq("local-merchant-account-id"), eq(webUrl),
                eq("sample-correlation-id"), any(LocalPaymentBrowserSwitchResultCallback.class));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSucceeds_sendsResultToCallback()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .integration("custom")
                .sessionId("session-id")
                .build();
        when(dataCollector.getClientMetadataId(any(Context.class),
                same(payPalEnabledConfig))).thenReturn("client-metadata-id");

        LocalPaymentNonce successNonce = LocalPaymentNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(successNonce)
                .build();

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        verify(localPaymentBrowserSwitchResultCallback).onResult(same(successNonce), isNull());
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(LocalPaymentNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)))
                .build();

        when(dataCollector.getClientMetadataId(any(Context.class),
                same(payPalEnabledConfig))).thenReturn("client-metadata-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.tokenize.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_onConfigurationError_returnsError()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        Exception configError = new Exception("config error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(dataCollector.getClientMetadataId(activity, payPalEnabledConfig)).thenReturn(
                "sample-correlation-id");

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);
        verify(localPaymentBrowserSwitchResultCallback).onResult(null, configError);
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndUserCancels_notifiesCallbackAndSendsAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult(isNull(),
                exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenResultCANCELED_sendsAnalyticsEvent()
            throws JSONException {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));
        LocalPaymentBrowserSwitchResult localPaymentBrowserSwitchResult =
                new LocalPaymentBrowserSwitchResult(browserSwitchResult);

        LocalPaymentClient sut =
                new LocalPaymentClient(braintreeClient, dataCollector,
                        localPaymentApi);

        sut.onBrowserSwitchResult(activity, localPaymentBrowserSwitchResult,
                localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult(isNull(),
                exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    private LocalPaymentRequest getIdealLocalPaymentRequest() {
        PostalAddress address = new PostalAddress();
        address.setStreetAddress("836486 of 22321 Park Lake");
        address.setExtendedAddress("Apt 2");
        address.setCountryCodeAlpha2("NL");
        address.setLocality("Den Haag");
        address.setRegion("CA");
        address.setPostalCode("2585 GJ");

        LocalPaymentRequest request = new LocalPaymentRequest();
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
