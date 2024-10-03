package com.braintreepayments.api.paypal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BrowserSwitchFinalResult;
import com.braintreepayments.api.BrowserSwitchOptions;
import com.braintreepayments.api.core.AnalyticsEventParams;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
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
    public void createPaymentAuthRequest_callsBackPayPalResponse_sendsStartedAnalytics() throws JSONException {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        );
        PayPalInternalClient payPalInternalClient =
            new MockPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        PayPalPaymentAuthRequestParams paymentAuthRequestCaptured =
            ((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        BrowserSwitchOptions browserSwitchOptions = paymentAuthRequestCaptured.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.PAYPAL.getCode(), browserSwitchOptions.getRequestCode());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());

        assertEquals(Uri.parse("https://example.com/approval/url"), browserSwitchOptions.getUrl());

        JSONObject metadata = browserSwitchOptions.getMetadata();
        assertEquals("https://example.com/approval/url", metadata.get("approval-url"));
        assertEquals("https://example.com/success/url", metadata.get("success-url"));
        assertEquals("billing-agreement", metadata.get("payment-type"));
        assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"));
        assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"));
        assertEquals("paypal-browser", metadata.get("source"));

        verify(braintreeClient).sendAnalyticsEvent(
            PayPalAnalytics.TOKENIZATION_STARTED,
            new AnalyticsEventParams(null, null, true, null, null, null)
        );
    }

    @Test
    public void createPaymentAuthRequest_whenLaunchesBrowserSwitchAsNewTaskEnabled_setsNewTaskOption() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        );

        PayPalInternalClient payPalInternalClient =
            new MockPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig)
                .launchesBrowserSwitchAsNewTask(true).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        assertTrue(((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams().getBrowserSwitchOptions().isLaunchAsNewTask());
    }

    @Test
    public void createPaymentAuthRequest_setsAppLinkReturnUrl() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        );

        PayPalInternalClient payPalInternalClient =
            new MockPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig)
                .appLinkReturnUri(Uri.parse("www.example.com"))
                .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        assertEquals(braintreeClient.getAppLinkReturnUri(),
            ((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams().getBrowserSwitchOptions().getAppLinkUri());
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalNotEnabled_returnsError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalDisabledConfig).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00", true),
            paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals("PayPal is not enabled. " +
                "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4 " +
                "for more information.",
            ((PayPalPaymentAuthRequest.Failure) request).getError().getMessage());

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setVaultRequest(false);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params);
    }

    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_whenConfigError_forwardsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        Exception authError = new Exception("Error fetching auth");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(authError)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00", true), paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(authError, ((PayPalPaymentAuthRequest.Failure) request).getError());

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setVaultRequest(false);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params);
    }

    @Test
    public void requestBillingAgreement_whenConfigError_forwardsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        Exception authError = new Exception("Error fetching auth");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(authError)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, new PayPalVaultRequest(true), paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(authError, ((PayPalPaymentAuthRequest.Failure) request).getError());

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setVaultRequest(true);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params);
    }


    @Test
    public void createPaymentAuthRequest_whenVaultRequest_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest),
            any(PayPalInternalClientCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00", true);

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest),
            any(PayPalInternalClientCallback.class));
    }

    @Test
    public void createPaymentAuthRequest_whenVaultRequest_sendsAppSwitchStartedEvent() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        payPalVaultRequest.setUserAuthenticationEmail("some@email.com");
        payPalVaultRequest.setEnablePayPalAppSwitch(true);
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
                payPalVaultRequest,
                null,
                "https://example.com/approval/url",
                "sample-client-metadata-id",
                null,
                "https://example.com/success/url"
        );
        PayPalInternalClient payPalInternalClient =
                new MockPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                        .build();

        when(payPalInternalClient.isDeepLinkSupportedByPayPalApp(activity)).thenReturn(true);
        when(payPalInternalClient.isAppSwitchEnabled(payPalVaultRequest)).thenReturn(true);

        BraintreeClient braintreeClient =
                new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        PayPalPaymentAuthRequestParams paymentAuthRequestCaptured =
                ((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams();

        BrowserSwitchOptions browserSwitchOptions = paymentAuthRequestCaptured.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.PAYPAL.getCode(), browserSwitchOptions.getRequestCode());
        assertFalse(browserSwitchOptions.isLaunchAsNewTask());


        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setVaultRequest(true);
        params.setLinkType("universal");
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_STARTED, params);
    }

    @Test
    public void tokenize_withBillingAgreement_tokenizesResponseOnSuccess() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
            new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "billing-agreement"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
            browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(),
            any(PayPalInternalTokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");
        JSONObject expectedPayPalTokenizePayload =
            new JSONObject().put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject().put("webURL", approvalUrl))
                .put("intent", "authorize").put("response_type", "web");

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }

    @Test
    public void tokenize_withOneTimePayment_tokenizesResponseOnSuccess() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
            new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
           browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(),
            any(PayPalInternalTokenizeCallback.class));

        PayPalAccount payPalAccount = captor.getValue();
        JSONObject tokenizePayload = payPalAccount.buildJSON();
        assertEquals("sample-merchant-account-id", tokenizePayload.get("merchant_account_id"));

        JSONObject payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount");
        JSONObject expectedPayPalTokenizePayload =
            new JSONObject().put("correlationId", "sample-client-metadata-id")
                .put("client", new JSONObject())
                .put("response", new JSONObject().put("webURL", approvalUrl))
                .put("intent", "authorize").put("response_type", "web")
                .put("options", new JSONObject().put("validate", false));

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true);
    }

    @Test
    public void tokenize_whenCancelUriReceived_notifiesCancellationAndSendsAnalyticsEvent()
        throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl = "sample-scheme://onetouch/v1/cancel";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
            new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
            browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Cancel);

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setVaultRequest(false);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params);
    }

    @Test
    public void tokenize_whenPayPalInternalClientTokenizeResult_callsBackResult()
        throws JSONException {
        PayPalAccountNonce payPalAccountNonce = mock(PayPalAccountNonce.class);
        PayPalInternalClient payPalInternalClient =
            new MockPayPalInternalClientBuilder().tokenizeSuccess(payPalAccountNonce).build();

        String approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
            new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
           browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Success);
        assertEquals(payPalAccountNonce, ((PayPalResult.Success) result).getNonce());

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setPayPalContextId("EC-HERMES-SANDBOX-EC-TOKEN");
        params.setVaultRequest(false);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params);
    }

    @Test
    public void tokenize_whenPayPalInternalClientTokenizeResult_sendsAppSwitchSucceededEvents()
            throws JSONException {
        PayPalAccountNonce payPalAccountNonce = mock(PayPalAccountNonce.class);
        PayPalInternalClient payPalInternalClient =
                new MockPayPalInternalClientBuilder().tokenizeSuccess(payPalAccountNonce).build();

        String approvalUrl =
                "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN&switch_initiated_time=17166111926211";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
                new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                        .put("merchant-account-id", "sample-merchant-account-id")
                        .put("intent", "authorize").put("approval-url", approvalUrl)
                        .put("success-url", "https://example.com/success")
                        .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
                browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Success);
        assertEquals(payPalAccountNonce, ((PayPalResult.Success) result).getNonce());

        AnalyticsEventParams params = new AnalyticsEventParams();
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.HANDLE_RETURN_STARTED, params);
        params.setPayPalContextId("EC-HERMES-SANDBOX-EC-TOKEN");
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_SUCCEEDED, params);
    }

    @Test
    public void tokenize_whenPayPalNotEnabled_sendsAppSwitchFailedEvents() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl = "https://some-scheme/onetouch/v1/cancel?switch_initiated_time=17166111926211";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
                new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                        .put("merchant-account-id", "sample-merchant-account-id")
                        .put("intent", "authorize").put("approval-url", "https://some-scheme/onetouch/v1/cancel?token=SOME-BA&switch_initiated_time=17166111926211")
                        .put("success-url", "https://example.com/cancel")
                        .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
                browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setPayPalContextId("SOME-BA");
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_FAILED, params);
    }

    @Test
    public void tokenize_whenCancelUriReceived_sendsAppSwitchCanceledEvents()
            throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String approvalUrl = "https://some-scheme/onetouch/v1/cancel?switch_initiated_time=17166111926211";

        BrowserSwitchFinalResult.Success browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(
                new JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                        .put("merchant-account-id", "sample-merchant-account-id")
                        .put("intent", "authorize").put("approval-url", approvalUrl)
                        .put("success-url", "https://example.com/success")
                        .put("payment-type", "single-payment"));

        Uri uri = Uri.parse(approvalUrl);
        when(browserSwitchResult.getReturnUrl()).thenReturn(uri);

        PayPalPaymentAuthResult.Success payPalPaymentAuthResult = new PayPalPaymentAuthResult.Success(
                browserSwitchResult);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Cancel);

        AnalyticsEventParams params = new AnalyticsEventParams();
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, params);
    }
}
