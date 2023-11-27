package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
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
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(RobolectricTestRunner.class)
public class PayPalClientUnitTest {

    private FragmentActivity activity;
    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    private PayPalTokenizeCallback payPalTokenizeCallback;
    private PayPalPaymentAuthCallback paymentAuthCallback;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        payPalTokenizeCallback = mock(PayPalTokenizeCallback.class);
        paymentAuthCallback = mock(PayPalPaymentAuthCallback.class);
    }

    @Test
    public void createPaymentAuthRequest_callsBackPayPalResponse() throws JSONException {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequest paymentAuthRequest = new PayPalPaymentAuthRequest(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onResult(captor.capture(), isNull());

        PayPalPaymentAuthRequest paymentAuthRequestCaptured = captor.getValue();

        BrowserSwitchOptions browserSwitchOptions = paymentAuthRequestCaptured.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.PAYPAL, browserSwitchOptions.getRequestCode());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());

        assertEquals(Uri.parse("https://example.com/approval/url"), browserSwitchOptions.getUrl());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("https://example.com/approval/url", metadata.get("approval-url"));
        assertEquals("https://example.com/success/url", metadata.get("success-url"));
        assertEquals("billing-agreement", metadata.get("payment-type"));
        assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"));
        assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"));
        assertEquals("paypal-browser", metadata.get("source"));
    }

    @Test
    public void createPaymentAuthRequest_whenLaunchesBrowserSwitchAsNewTaskEnabled_setsNewTaskOption() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequest paymentAuthRequest = new PayPalPaymentAuthRequest(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .launchesBrowserSwitchAsNewTask(true)
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onResult(captor.capture(), isNull());

        PayPalPaymentAuthRequest paymentAuthRequestCaptured = captor.getValue();
        assertTrue(paymentAuthRequestCaptured.getBrowserSwitchOptions().isLaunchAsNewTask());
    }

    @Test
    public void createPaymentAuthRequest_whenVaultRequest_sendsAnalyticsEvents() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequest paymentAuthRequest = new PayPalPaymentAuthRequest(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent(
                "paypal.billing-agreement.browser-switch.started");
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalNotEnabled_returnsError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00"),
                paymentAuthCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(paymentAuthCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4 " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenDeviceCantPerformBrowserSwitch_returnsError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .browserSwitchAssertionError(new BrowserSwitchException("browser switch error"))
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00"),
                paymentAuthCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(paymentAuthCallback).onResult(isNull(), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                        "defines the same browser switch url as this app. See " +
                        "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                        "for the correct configuration: browser switch error",
                errorCaptor.getValue().getMessage());
    }


    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_sendsBrowserSwitchStartAnalyticsEvent() {
        PayPalCheckoutRequest payPalCheckoutRequest = new PayPalCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequest paymentAuthRequest = new PayPalPaymentAuthRequest(payPalCheckoutRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalCheckoutRequest, paymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.started");
    }

    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_sendsPayPalPayLaterOfferedAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
        request.setShouldOfferPayLater(true);
        sut.createPaymentAuthRequest(activity, request, paymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.paylater.offered");
    }

    @Test
    public void createPaymentAuthRequest_whenVaultRequest_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest),
                any(PayPalInternalClientCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest),
                any(PayPalInternalClientCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_sendsPayPalCreditOfferedAnalyticsEvent() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest();
        payPalRequest.setShouldOfferCredit(true);

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
    }

    @Test
    public void tokenize_whenResultNull_callsBackError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(null, payPalTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalTokenizeCallback).onResult(isNull(), captor.capture());

        assertNotNull(captor.getValue());
        assertEquals("PayPalBrowserSwitchResult cannot be null", captor.getValue().getMessage());
    }

    @Test
    public void tokenize_whenErrorNotNull_callsBackError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        Exception expectedError = new Exception("error");
        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(expectedError);
        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalTokenizeCallback).onResult(isNull(), captor.capture());

        assertNotNull(captor.getValue());
        assertSame(expectedError, captor.getValue());
    }

    @Test
    public void tokenize_whenResultAndErrorNull_callsBackUnexpectedError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        PayPalPaymentAuthResult payPalPaymentAuthResult = new PayPalPaymentAuthResult(
                (BrowserSwitchResult) null);
        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalTokenizeCallback).onResult(isNull(), captor.capture());

        assertNotNull(captor.getValue());
        assertEquals("An unexpected error occurred", captor.getValue().getMessage());

    }

    @Test
    public void tokenize_withBillingAgreement_tokenizesResponseOnSuccess()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "billing-agreement")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(),
                any(PayPalInternalTokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");
        JSONObject expectedPayPalTokenizePayload = new JSONObject()
                .put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject()
                        .put("webURL", approvalUrl))
                .put("intent", "authorize")
                .put("response_type", "web");

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }

    @Test
    public void tokenize_withOneTimePayment_tokenizesResponseOnSuccess()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(),
                any(PayPalInternalTokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");
        JSONObject expectedPayPalTokenizePayload = new JSONObject()
                .put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject()
                        .put("webURL", approvalUrl))
                .put("intent", "authorize")
                .put("response_type", "web")
                .put("options", new JSONObject()
                        .put("validate", false)
                );

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }

    @Test
    public void tokenize_withBillingAgreement_sendsAnalyticsEvents()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "billing-agreement")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void tokenize_oneTimePayment_sendsAnalyticsEvents() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent(
                "paypal.single-payment.browser-switch.succeeded");
    }

    @Test
    public void tokenize_whenPayPalCreditPresent_sendsAnalyticsEvents()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(PayPalAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)))
                .build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.credit.accepted");
    }

    @Test
    public void tokenize_whenCancelUriReceived_notifiesCancellationAndSendsAnalyticsEvent()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl = "sample-scheme://onetouch/v1/cancel";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalTokenizeCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());
        assertTrue(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent(
                eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void tokenize_whenBrowserSwitchCanceled_forwardsExceptionAndSendsAnalyticsEvent() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject());

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalTokenizeCallback).onResult(isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());
        assertFalse(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent(
                eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void tokenize_whenPayPalInternalClientTokenizeResult_callsBackResult()
            throws JSONException {
        PayPalAccountNonce payPalAccountNonce = mock(PayPalAccountNonce.class);
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(payPalAccountNonce)
                .build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.SUCCESS);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject()
                .put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize")
                .put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")
        );

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(uri);

        PayPalPaymentAuthResult payPalPaymentAuthResult =
                new PayPalPaymentAuthResult(browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut =
                new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);
        verify(payPalTokenizeCallback).onResult(same(payPalAccountNonce), isNull());
    }
}