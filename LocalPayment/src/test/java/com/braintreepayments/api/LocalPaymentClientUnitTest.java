package com.braintreepayments.api;

import android.content.Context;
import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.ibm.icu.impl.Assert.fail;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentClientUnitTest {

    private FragmentActivity activity;
    private LocalPaymentStartCallback localPaymentStartCallback;
    private LocalPaymentBrowserSwitchResultCallback localPaymentBrowserSwitchResultCallback;

    private BraintreeClient braintreeClient;
    private PayPalDataCollector payPalDataCollector;
    private LocalPaymentApi localPaymentApi;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        localPaymentStartCallback = mock(LocalPaymentStartCallback.class);
        localPaymentBrowserSwitchResultCallback = mock(LocalPaymentBrowserSwitchResultCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        payPalDataCollector = mock(PayPalDataCollector.class);
        localPaymentApi = mock(LocalPaymentApi.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void startPayment_success_updatesOriginalRequestAndNotifiesListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<LocalPaymentResult> transactionCaptor = ArgumentCaptor.forClass(LocalPaymentResult.class);
        verify(localPaymentStartCallback).onResult(transactionCaptor.capture(), (Exception) isNull());

        LocalPaymentResult transaction = transactionCaptor.getValue();
        assertSame(request, transaction.getRequest());
        assertEquals("https://checkout.paypal.com/latinum?token=payment-token", transaction.getApprovalUrl());
        assertEquals("local-payment-id-123", transaction.getPaymentId());
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .createPaymentMethodSuccess(mock(LocalPaymentResult.class))
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.start-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.create.succeeded");
    }

    @Test
    public void startPayment_configurationFetchError_forwardsErrorToListener() {
        Exception configException = new Exception(("Configuration not fetched"));
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configurationError(configException)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
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

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.start-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.initiate.failed");
    }

    @Test
    public void startPayment_callsExceptionListener_configurationFetchError() {
        Exception configError = new Exception("config fetch error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTErrorResponse(configError)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(null, configError);
    }

    @Test
    public void startPayment_callsExceptionListener_payPalDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("Local payments are not enabled for this merchant.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_amountIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setAmount(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_paymentTypeIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.setPaymentType(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_localPaymentRequestIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        sut.startPayment(null, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentResult) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("A LocalPaymentRequest is required.", exception.getMessage());
    }

    @Test
    public void startPayment_throwsError_callbackIsNull() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        try {
            sut.startPayment(request, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentCallback is required.", exception.getMessage());
        }
    }

    @Test
    public void approvePayment_startsBrowserWithProperRequestCode() throws JSONException, BrowserSwitchException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approvePayment(activity, transaction);

        ArgumentCaptor<BrowserSwitchOptions> optionsCaptor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), optionsCaptor.capture());

        BrowserSwitchOptions browserSwitchOptions = optionsCaptor.getValue();
        assertEquals(BraintreeRequestCodes.LOCAL_PAYMENT, browserSwitchOptions.getRequestCode());
        assertEquals(Uri.parse("https://sample.com/approval?token=sample-token"), browserSwitchOptions.getUrl());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        JSONObject expectedMetadata = new JSONObject()
                .put("merchant-account-id", "local-merchant-account-id")
                .put("payment-type", "ideal");

        JSONAssert.assertEquals(expectedMetadata, metadata, true);
    }

    @Test
    public void approvePayment_sendsAnalyticsEvents() throws JSONException, BrowserSwitchException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        sut.approvePayment(activity, transaction);
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.initiate.succeeded");
    }

    @Test
    public void approvePayment_whenActivityIsNull_throwsError() throws BrowserSwitchException, JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentResult transaction = new LocalPaymentResult(request, approvalUrl, "payment-id");

        try {
            sut.approvePayment(null, transaction);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A FragmentActivity is required.", exception.getMessage());
        }
    }

    @Test
    public void approvePayment_whenTransactionIsNull_throwsError() throws BrowserSwitchException, JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        try {
            sut.approvePayment(activity, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentTransaction is required.", exception.getMessage());
        }
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_uriNull_notifiesListenerOfErrorAlongWithAnalyticsEvent() throws JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), exceptionCaptor.capture());

        Exception exception = exceptionCaptor.getValue();
        assertTrue(exception instanceof BraintreeException);

        String expectedMessage = "LocalPayment encountered an error, return URL is invalid.";
        assertEquals(expectedMessage, exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch-response.invalid");
    }

    @Test
    public void onBrowserSwitchResult_whenPostFailure_notifiesListenerOfErrorAlongWithAnalyticsEvent() throws JSONException {
        Exception postError = new Exception("POST failed");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTErrorResponse(postError)
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeError(postError)
                .build();

        when(payPalDataCollector.getClientMetadataId(activity)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), same(postError));
        verify(braintreeClient).sendAnalyticsEvent(eq("ideal.local-payment.tokenize.failed"));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndSuccessful_tokenizesWithLocalPaymentApi() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(payPalDataCollector.getClientMetadataId(activity)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        verify(localPaymentApi).tokenize(eq("local-merchant-account-id"), eq(webUrl), eq("sample-correlation-id"), any(LocalPaymentBrowserSwitchResultCallback.class));
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSucceeds_sendsResultToListener() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .integration("custom")
                .sessionId("session-id")
                .build();
        when(payPalDataCollector.getClientMetadataId(any(Context.class))).thenReturn("client-metadata-id");
        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)))
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<LocalPaymentNonce> resultCaptor = ArgumentCaptor.forClass(LocalPaymentNonce.class);
        // TODO: update to listener
        verify(localPaymentBrowserSwitchResultCallback).onResult(resultCaptor.capture(), (Exception) isNull());

        LocalPaymentNonce localPaymentNonce = resultCaptor.getValue();
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", localPaymentNonce.getString());
        assertEquals("084afbf1db15445587d30bc120a23b09", localPaymentNonce.getClientMetadataId());
        assertEquals("jon@getbraintree.com", localPaymentNonce.getEmail());
        assertEquals("Jon", localPaymentNonce.getGivenName());
        assertEquals("Doe", localPaymentNonce.getSurname());
        assertEquals("9KQSUZTL7YZQ4", localPaymentNonce.getPayerId());

        PostalAddress shippingAddress = localPaymentNonce.getShippingAddress();
        assertEquals("Jon Doe", shippingAddress.getRecipientName());
        assertEquals("836486 of 22321 Park Lake", shippingAddress.getStreetAddress());
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSuccess_sendsAnalyticsEvent() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        LocalPaymentApi localPaymentApi = new MockLocalPaymentApiBuilder()
                .tokenizeSuccess(LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)))
                .build();

        when(payPalDataCollector.getClientMetadataId(any(Context.class))).thenReturn("client-metadata-id");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.tokenize.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndUserCancels_notifiesListenerAndSendsAnalyticsEvent() throws JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-cancel?paymentToken=canceled";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenResultCANCELED_sendsAnalyticsEvent() throws JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof UserCanceledException);
        assertEquals("User canceled Local Payment.", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchResultIsNull_returnsExceptionToCallback() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector, localPaymentApi);

        sut.onBrowserSwitchResult(activity, null, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), exceptionCaptor.capture());

        Exception exception = exceptionCaptor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("BrowserSwitchResult cannot be null", exception.getMessage());
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