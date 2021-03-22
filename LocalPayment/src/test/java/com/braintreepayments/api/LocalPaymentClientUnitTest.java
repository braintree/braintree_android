package com.braintreepayments.api;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        localPaymentStartCallback = mock(LocalPaymentStartCallback.class);
        localPaymentBrowserSwitchResultCallback = mock(LocalPaymentBrowserSwitchResultCallback.class);

        braintreeClient = mock(BraintreeClient.class);
        payPalDataCollector = mock(PayPalDataCollector.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void startPayment_postsParameters() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        when(braintreeClient.getReturnUrlScheme()).thenReturn("sample-scheme");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        sut.startPayment(getIdealLocalPaymentRequest(), localPaymentStartCallback);

        String expectedPath = "/v1/local_payments/create";
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(braintreeClient).sendPOST(eq(expectedPath), bodyCaptor.capture(), any(HttpResponseCallback.class));

        String requestBody = bodyCaptor.getValue();
        JSONObject json = new JSONObject(requestBody);
        assertEquals("Doe", json.getString("lastName"));
        assertEquals("1.10", json.getString("amount"));
        assertEquals("Den Haag", json.getString("city"));
        assertEquals("2585 GJ", json.getString("postalCode"));
        assertEquals("sale", json.getString("intent"));
        assertEquals("Jon", json.getString("firstName"));
        assertEquals("639847934", json.getString("phone"));
        assertEquals("NL", json.getString("countryCode"));
        assertEquals("EUR", json.getString("currencyIsoCode"));
        assertEquals("ideal", json.getString("fundingSource"));
        assertEquals("jon@getbraintree.com", json.getString("payerEmail"));
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"));
        assertEquals("Apt 2", json.getString("line2"));
        assertEquals("CA", json.getString("state"));
        assertEquals("local-merchant-account-id", json.getString("merchantAccountId"));
        assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"));
        String expectedCancelUrl = Uri.parse("sample-scheme://local-payment-cancel").toString();
        String expectedReturnUrl = Uri.parse("sample-scheme://local-payment-success").toString();
        assertEquals(expectedCancelUrl, json.getString("cancelUrl"));
        assertEquals(expectedReturnUrl, json.getString("returnUrl"));
    }

    @Test
    public void startPayment_success_updatesOriginalRequestAndNotifiesListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<LocalPaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(LocalPaymentTransaction.class);
        verify(localPaymentStartCallback).onResult(transactionCaptor.capture(), (Exception) isNull());

        LocalPaymentTransaction transaction = transactionCaptor.getValue();
        assertSame(request, transaction.getRequest());
        assertEquals("https://checkout.paypal.com/latinum?token=payment-token", transaction.getApprovalUrl());
        assertEquals("local-payment-id-123", transaction.getPaymentId());
    }

    @Test
    public void startPayment_success_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
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

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult((LocalPaymentTransaction) isNull(), same(configException));
    }

    @Test
    public void startPayment_localPaymentsError_sendsAnalyticsEvents() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTSuccessfulResponse(Fixtures.ERROR_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.start-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.initiate.failed");
    }

    @Test
    public void startPayment_httpError_notifiesListener() {
        Exception httpError = new Exception("http error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTErrorResponse(httpError)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(null, httpError);
    }

    @Test
    public void startPayment_httpError_sendsAnalyticsEvents() {
        Exception httpError = new Exception("http error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .sendPOSTErrorResponse(httpError)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
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

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        verify(localPaymentStartCallback).onResult(null, configError);
    }

    @Test
    public void startPayment_callsExceptionListener_payPalDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentTransaction) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof ConfigurationException);
        assertEquals("Local payments are not enabled for this merchant.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_amountIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.amount(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentTransaction) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_paymentTypeIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        request.paymentType(null);

        sut.startPayment(request, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentTransaction) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("LocalPaymentRequest is invalid, paymentType and amount are required.", exception.getMessage());
    }

    @Test
    public void startPayment_callsExceptionListener_localPaymentRequestIsNull() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        sut.startPayment(null, localPaymentStartCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentStartCallback).onResult((LocalPaymentTransaction) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("A LocalPaymentRequest is required.", exception.getMessage());
    }

    @Test
    public void startPayment_throwsError_callbackIsNull() {
        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        try {
            sut.startPayment(request, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentCallback is required.", exception.getMessage());
        }
    }

    @Test
    public void approvePayment_startsBrowserWithProperRequestCode() throws JSONException, BrowserSwitchException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentTransaction transaction = new LocalPaymentTransaction(request, approvalUrl, "payment-id");

        sut.approveTransaction(activity, transaction);

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
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentTransaction transaction = new LocalPaymentTransaction(request, approvalUrl, "payment-id");

        sut.approveTransaction(activity, transaction);
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.initiate.succeeded");
    }

    @Test
    public void approveTransaction_whenActivityIsNull_throwsError() throws BrowserSwitchException, JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        LocalPaymentRequest request = getIdealLocalPaymentRequest();
        String approvalUrl = "https://sample.com/approval?token=sample-token";
        LocalPaymentTransaction transaction = new LocalPaymentTransaction(request, approvalUrl, "payment-id");

        try {
            sut.approveTransaction(null, transaction);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A FragmentActivity is required.", exception.getMessage());
        }
    }

    @Test
    public void approveTransaction_whenTransactionIsNull_throwsError() throws BrowserSwitchException, JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        try {
            sut.approveTransaction(activity, null);
            fail("Should throw");
        } catch (RuntimeException exception) {
            assertEquals("A LocalPaymentTransaction is required.", exception.getMessage());
        }
    }

    @Test
    public void onBrowserSwitchResult_whenResultOK_uriNull_notifiesListenerOfErrorAlongWithAnalyticsEvent() throws JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

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

        when(payPalDataCollector.getClientMetadataId(activity)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

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
    public void onBrowserSwitchResult_whenResultOKAndSuccessful_postsTokenizationRequest() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("sample-session-id")
                .integration("sample-integration-type")
                .build();
        when(payPalDataCollector.getClientMetadataId(activity)).thenReturn("sample-correlation-id");

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        String expectedUrl = "/v1/payment_methods/paypal_accounts";

        verify(braintreeClient).sendPOST(eq(expectedUrl), bodyCaptor.capture(), any(HttpResponseCallback.class));
        String requestBody = bodyCaptor.getValue();

        JSONObject expectedJSON = new JSONObject();
        expectedJSON.put("merchant_account_id", "local-merchant-account-id");

        JSONObject paypalAccount = new JSONObject()
                .put("intent", "sale")
                .put("response", new JSONObject().put("webURL", webUrl))
                .put("options", new JSONObject().put("validate", false))
                .put("response_type", "web")
                .put("correlation_id", "sample-correlation-id");
        expectedJSON.put("paypal_account", paypalAccount);

        JSONObject metaData = new JSONObject()
                .put("source", "client")
                .put("integration", "sample-integration-type")
                .put("sessionId", "sample-session-id");
        expectedJSON.put("_meta", metaData);

        JSONAssert.assertEquals(expectedJSON, new JSONObject(requestBody), true);
    }

    @Test
    public void onBrowserSwitchResult_whenResultOKAndTokenizationSucceeds_sendsResultToListener() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        String webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId";
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(Uri.parse(webUrl));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<LocalPaymentNonce> resultCaptor = ArgumentCaptor.forClass(LocalPaymentNonce.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult(resultCaptor.capture(), (Exception) isNull());

        LocalPaymentNonce localPaymentNonce = resultCaptor.getValue();
        assertEquals("PayPalAccount", localPaymentNonce.getTypeLabel());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", localPaymentNonce.getNonce());
        assertEquals("PayPal", localPaymentNonce.getDescription());
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
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
                .build();

        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

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
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

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
        assertTrue(cancelException instanceof BraintreeException);
        assertEquals("user canceled", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenResultCANCELED_sendsAnalyticsEvent() throws JSONException {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("payment-type", "ideal")
                .put("merchant-account-id", "local-merchant-account-id"));

        sut.onBrowserSwitchResult(activity, browserSwitchResult, localPaymentBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(localPaymentBrowserSwitchResultCallback).onResult((LocalPaymentNonce) isNull(), exceptionCaptor.capture());

        Exception cancelException = exceptionCaptor.getValue();
        assertTrue(cancelException instanceof BraintreeException);
        assertEquals("system canceled", cancelException.getMessage());
        verify(braintreeClient).sendAnalyticsEvent("ideal.local-payment.webswitch.canceled");
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchResultIsNull_returnsExceptionToCallback() {
        LocalPaymentClient sut = new LocalPaymentClient(braintreeClient, payPalDataCollector);

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
        request.paymentType("ideal");
        request.amount("1.10");
        request.address(address);
        request.phone("639847934");
        request.email("jon@getbraintree.com");
        request.givenName("Jon");
        request.surname("Doe");
        request.shippingAddressRequired(false);
        request.merchantAccountId("local-merchant-account-id");
        request.currencyCode("EUR");
        request.paymentTypeCountryCode("NL");

        return request;
    }
}