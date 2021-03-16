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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PayPalClientUnitTest {

    private FragmentActivity context;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    private PayPalBrowserSwitchResultCallback payPalBrowserSwitchResultCallback;
    private PayPalFlowStartedCallback payPalFlowStartedCallback;

    @Before
    public void beforeEach() throws JSONException {
        context = mock(FragmentActivity.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        payPalBrowserSwitchResultCallback = mock(PayPalBrowserSwitchResultCallback.class);
        payPalFlowStartedCallback = mock(PayPalFlowStartedCallback.class);
    }

//    @Test
//    public void requestBillingAgreement_throwsExceptionWhenAmountIsIncluded() {
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
//        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
//        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
//
//        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
//        sut.requestBillingAgreement(context, new PayPalRequest().amount("1.00"), payPalFlowStartedCallback);
//
//        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
//        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
//        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
//        assertEquals("There must be no amount specified for the Billing Agreement flow", errorCaptor.getValue().getMessage());
//    }

    @Test
    public void requestBillingAgreement_whenPayPalNotEnabled_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestBillingAgreement(context, new PayPalRequest(), payPalFlowStartedCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void requestBillingAgreement_whenDeviceCantPerformBrowserSwitch_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestBillingAgreement(context, new PayPalRequest(), payPalFlowStartedCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", errorCaptor.getValue().getMessage());
    }

    @Test
    public void requestBillingAgreement_startsBrowser() throws JSONException, BrowserSwitchException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        PayPalResponse payPalResponse = new PayPalResponse()
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .isBillingAgreement(true)
                .clientMetadataId("sample-client-metadata-id")
                .merchantAccountId("sample-merchant-account-id")
                .intent("authorize");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .success(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        PayPalRequest payPalRequest = new PayPalRequest();
        sut.requestBillingAgreement(context, payPalRequest, payPalFlowStartedCallback);

        verify(payPalFlowStartedCallback).onResult(null);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(context), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(BraintreeRequestCodes.PAYPAL, browserSwitchOptions.getRequestCode());

        assertEquals(Uri.parse("https://example.com/approval/url"), browserSwitchOptions.getUrl());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("https://example.com/approval/url", metadata.get("approval-url"));
        assertEquals("https://example.com/success/url", metadata.get("success-url"));
        assertEquals("billing-agreement", metadata.get("payment-type"));
        assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"));
        assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"));
        assertEquals("paypal-browser", metadata.get("source"));
        assertEquals("authorize", metadata.get("intent"));
    }

    @Test
    public void requestBillingAgreement_sendsAnalyticsEvents() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        PayPalResponse payPalResponse = new PayPalResponse()
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .isBillingAgreement(true)
                .clientMetadataId("sample-client-metadata-id")
                .merchantAccountId("sample-merchant-account-id")
                .intent("authorize");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .success(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(true)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        PayPalRequest payPalRequest = new PayPalRequest();
        sut.requestBillingAgreement(context, payPalRequest, payPalFlowStartedCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.started");
    }

    @Test
    public void requestOneTimePayment_startsBrowser() throws JSONException, BrowserSwitchException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        PayPalResponse payPalResponse = new PayPalResponse()
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .isBillingAgreement(false)
                .clientMetadataId("sample-client-metadata-id")
                .merchantAccountId("sample-merchant-account-id")
                .intent("authorize");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .success(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");
        sut.requestOneTimePayment(context, payPalRequest, payPalFlowStartedCallback);

        verify(payPalFlowStartedCallback).onResult(null);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(context), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertEquals(BraintreeRequestCodes.PAYPAL, browserSwitchOptions.getRequestCode());

        assertEquals(Uri.parse("https://example.com/approval/url"), browserSwitchOptions.getUrl());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("https://example.com/approval/url", metadata.get("approval-url"));
        assertEquals("https://example.com/success/url", metadata.get("success-url"));
        assertEquals("single-payment", metadata.get("payment-type"));
        assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"));
        assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"));
        assertEquals("paypal-browser", metadata.get("source"));
        assertEquals("authorize", metadata.get("intent"));
    }

    @Test
    public void requestOneTimePayment_whenPayPalNotEnabled_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestOneTimePayment(context, new PayPalCheckoutRequest("1.00"), payPalFlowStartedCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void requestOneTimePayment_whenDeviceCantPerformBrowserSwitch_throwsError() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestOneTimePayment(context, new PayPalCheckoutRequest("1.00"), payPalFlowStartedCallback);

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", errorCaptor.getValue().getMessage());
    }


    @Test
    public void requestOneTimePayment_sendsBrowserSwitchStartAnalyticsEvent() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();

        PayPalResponse payPalResponse = new PayPalResponse()
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .isBillingAgreement(false)
                .clientMetadataId("sample-client-metadata-id")
                .merchantAccountId("sample-merchant-account-id")
                .intent("authorize");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .success(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");
        sut.requestOneTimePayment(context, payPalRequest, payPalFlowStartedCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.started");
    }

    @Test
    public void requestOneTimePayment_sendsPayPalPayLaterOfferedAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
        request.setOfferPayLater(true);
        sut.requestOneTimePayment(context, request, payPalFlowStartedCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.paylater.offered");
    }

    @Test
    public void requestBillingAgreement_sendsPayPalRequestViaInternalClient() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalRequest payPalRequest = new PayPalRequest();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestBillingAgreement(context, payPalRequest, payPalFlowStartedCallback);

        verify(payPalInternalClient).sendRequest(same(context), same(payPalRequest), eq(true), any(PayPalInternalClientCallback.class));
    }

    @Test
    public void requestOneTimePayment_sendsPayPalRequestViaInternalClient() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
        sut.requestOneTimePayment(context, payPalRequest, payPalFlowStartedCallback);

        verify(payPalInternalClient).sendRequest(same(context), same(payPalRequest), eq(false), any(PayPalInternalClientCallback.class));
    }

    @Test
    public void requestBillingAgreement_sendsPayPalCreditOfferedAnalyticsEvent() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalRequest payPalRequest = new PayPalRequest();
        payPalRequest.setOfferCredit(true);
        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        sut.requestBillingAgreement(context, payPalRequest, payPalFlowStartedCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
    }

    @Test
    public void onBrowserSwitchResult_withBillingAgreement_tokenizesResponseOnSuccess() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        String approvalUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN";

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        ArgumentCaptor<PayPalAccountBuilder> captor = ArgumentCaptor.forClass(PayPalAccountBuilder.class);
        verify(tokenizationClient).tokenize(captor.capture(), any(PaymentMethodNonceCallback.class));

        PayPalAccountBuilder payPalAccountBuilder = captor.getValue();
        JSONObject tokenizePayload = new JSONObject(payPalAccountBuilder.build());
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
    public void onBrowserSwitchResult_withOneTimePayment_tokenizesResponseOnSuccess() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        String approvalUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        ArgumentCaptor<PayPalAccountBuilder> captor = ArgumentCaptor.forClass(PayPalAccountBuilder.class);
        verify(tokenizationClient).tokenize(captor.capture(), any(PaymentMethodNonceCallback.class));

        PayPalAccountBuilder payPalAccountBuilder = captor.getValue();
        JSONObject tokenizePayload = new JSONObject(payPalAccountBuilder.build());
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
    public void onBrowserSwitchResult_withBillingAgreement_sendsAnalyticsEvents() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        String approvalUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN";

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_oneTimePayment_sendsAnalyticsEvents() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        String approvalUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenPayPalCreditPresent_sendsAnalyticsEvents() throws JSONException {
        PayPalAccountNonce payPalAccountNonce = mock(PayPalAccountNonce.class);
        when(payPalAccountNonce.getCreditFinancing()).thenReturn(mock(PayPalCreditFinancing.class));

        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder()
                .successNonce(payPalAccountNonce)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        String approvalUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.credit.accepted");
    }

    @Test
    public void onBrowserSwitchResult_whenCancelUriReceived_notifiesCancellation() throws JSONException {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

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

        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalBrowserSwitchResultCallback).onResult((PayPalAccountNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof PayPalBrowserSwitchException);
        assertEquals("User cancelled.", exception.getMessage());
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchCanceled_forwardsExceptionAndSendsAnalyticsEvent() {
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject());
        sut.onBrowserSwitchResult(browserSwitchResult, payPalBrowserSwitchResultCallback);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(payPalBrowserSwitchResultCallback).onResult((PayPalAccountNonce) isNull(), captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof BraintreeException);
        assertEquals("User Canceled PayPal", exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(eq("paypal.single-payment.browser-switch.canceled"));
    }

//    @Test
//    public void requestOneTimePayment_throwsExceptionWhenNoAmountSet() {
//        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
//        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
//        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
//
//        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);
//        sut.requestOneTimePayment(context, new PayPalCheckoutRequest(), payPalFlowStartedCallback);
//
//        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
//        verify(payPalFlowStartedCallback).onResult(errorCaptor.capture());
//        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
//        assertEquals("An amount must be specified for the Single Payment flow.", errorCaptor.getValue().getMessage());
//    }

    @Test
    public void requestOneTimePayment_sendsPayPalCreditOfferedAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        TokenizationClient tokenizationClient = new MockTokenizationClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalClient sut = new PayPalClient(braintreeClient, tokenizationClient, payPalInternalClient);

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");
        payPalRequest.setOfferCredit(true);
        sut.requestOneTimePayment(context, payPalRequest, payPalFlowStartedCallback);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.credit.offered");
    }
}