package com.braintreepayments.api;

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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PayPalClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private PayPalListener listener;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    private PayPalBrowserSwitchResultCallback payPalBrowserSwitchResultCallback;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        listener = mock(PayPalListener.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        payPalBrowserSwitchResultCallback = mock(PayPalBrowserSwitchResultCallback.class);
    }

    @Test
    public void constructor_setsLifecycleObserver() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        ArgumentCaptor<PayPalLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        PayPalClient sut = new PayPalClient(fragment, braintreeClient);

        ArgumentCaptor<PayPalLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

        PayPalClient sut = new PayPalClient(activity, braintreeClient);

        ArgumentCaptor<PayPalLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(null, null, braintreeClient, payPalInternalClient);

        ArgumentCaptor<PayPalLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalLifecycleObserver.class);
        verify(lifecycle, never()).addObserver(captor.capture());
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultExists_deliversResultToListener_andSetsPendingResultNull() throws JSONException {
        PayPalAccountNonce nonce = mock(PayPalAccountNonce.class);
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(nonce)
                .build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

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

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.pendingBrowserSwitchResult = browserSwitchResult;
        sut.setListener(listener);

        verify(listener).onPayPalSuccess(same(nonce));
        verify(listener, never()).onPayPalFailure(any(Exception.class));
        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultDoesNotExist_doesNotInvokeListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.pendingBrowserSwitchResult = null;
        sut.setListener(listener);

        verify(listener, never()).onPayPalSuccess(any(PayPalAccountNonce.class));
        verify(listener, never()).onPayPalFailure(any(Exception.class));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void tokenizePayPalAccount_whenPayPalNotEnabled_returnsErroToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalVaultRequest());

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_whenDeviceCantPerformBrowserSwitch_returnsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalVaultRequest());

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_startsBrowser() throws JSONException, BrowserSwitchException {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalResponse payPalResponse = new PayPalResponse(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalVaultRequest);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
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
    public void tokenizePayPalAccount_whenDefaultDeepLinkHandlerEnabled_startsBrowserAsSingleTask() throws JSONException, BrowserSwitchException {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalResponse payPalResponse = new PayPalResponse(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .useDefaultDeepLinkHandler(true)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalVaultRequest);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

        BrowserSwitchOptions browserSwitchOptions = captor.getValue();
        assertTrue(browserSwitchOptions.isLaunchAsNewTask());
    }

    @Test
    public void tokenizePayPalAccount_sendsAnalyticsEvents() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalResponse payPalResponse = new PayPalResponse(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(true)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalVaultRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.started");
    }

    @Test
    public void requestOneTimePayment_startsBrowser() throws JSONException, BrowserSwitchException {
        PayPalCheckoutRequest payPalCheckoutRequest = new PayPalCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalResponse payPalResponse = new PayPalResponse(payPalCheckoutRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalCheckoutRequest);

        ArgumentCaptor<BrowserSwitchOptions> captor = ArgumentCaptor.forClass(BrowserSwitchOptions.class);
        verify(braintreeClient).startBrowserSwitch(same(activity), captor.capture());

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
    public void requestOneTimePayment_whenPayPalNotEnabled_returnsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalCheckoutRequest("1.00"));

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void requestOneTimePayment_whenDeviceCantPerformBrowserSwitch_returnsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalCheckoutRequest("1.00"));

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", errorCaptor.getValue().getMessage());
    }


    @Test
    public void requestOneTimePayment_sendsBrowserSwitchStartAnalyticsEvent() {
        PayPalCheckoutRequest payPalCheckoutRequest = new PayPalCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalResponse payPalResponse = new PayPalResponse(payPalCheckoutRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalCheckoutRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.started");
    }

    @Test
    public void requestOneTimePayment_sendsPayPalPayLaterOfferedAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        PayPalCheckoutRequest request = new PayPalCheckoutRequest("1.00");
        request.setShouldOfferPayLater(true);
        sut.tokenizePayPalAccount(activity, request);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.paylater.offered");
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalInternalClientCallback.class));
    }

    @Test
    public void requestOneTimePayment_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalCheckoutRequest payPalRequest = new PayPalCheckoutRequest("1.00");

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalInternalClientCallback.class));
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalCreditOfferedAnalyticsEvent() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest();
        payPalRequest.setShouldOfferCredit(true);

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
    }

    @Test
    public void onBrowserSwitchResult_withBillingAgreement_tokenizesResponseOnSuccess() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(), any(PayPalBrowserSwitchResultCallback.class));

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
    public void onBrowserSwitchResult_withOneTimePayment_tokenizesResponseOnSuccess() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<PayPalAccount> captor = ArgumentCaptor.forClass(PayPalAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(), any(PayPalBrowserSwitchResultCallback.class));

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
    public void onBrowserSwitchResult_withBillingAgreement_sendsAnalyticsEvents() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_oneTimePayment_sendsAnalyticsEvents() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenPayPalCreditPresent_sendsAnalyticsEvents() throws JSONException {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(PayPalAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)))
                .build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        verify(braintreeClient).sendAnalyticsEvent("paypal.credit.accepted");
    }

    @Test
    public void onBrowserSwitchResult_whenCancelUriReceived_notifiesCancellationAndSendsAnalyticsEvent() throws JSONException {
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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());
        assertTrue(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent(eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchCanceled_forwardsExceptionAndSendsAnalyticsEvent() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject());
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());
        assertFalse(((UserCanceledException) exception).isExplicitCancelation());

        verify(braintreeClient).sendAnalyticsEvent(eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void onBrowserSwitchResult_whenPayPalInternalClientTokenizeResult_forwardsResultToListener() throws JSONException {
        PayPalAccountNonce payPalAccountNonce = mock(PayPalAccountNonce.class);
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(payPalAccountNonce)
                .build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(browserSwitchResult);
        verify(listener).onPayPalSuccess(same(payPalAccountNonce));
    }

    @Test
    public void getBrowserSwitchResult_forwardsInvocationToBraintreeClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);

        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        BrowserSwitchResult result = sut.getBrowserSwitchResult(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void deliverBrowserSwitchResult_forwardsInvocationToBraintreeClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);

        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        BrowserSwitchResult result = sut.deliverBrowserSwitchResult(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void getBrowserSwitchResultFromCache_forwardsInvocationToBraintreeClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);

        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        BrowserSwitchResult result = sut.getBrowserSwitchResultFromCache(activity);
        assertSame(browserSwitchResult, result);
    }

    @Test
    public void deliverBrowserSwitchResultFromCache_forwardsInvocationToBraintreeClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);

        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.deliverBrowserSwitchResultFromCache(activity)).thenReturn(browserSwitchResult);

        PayPalClient sut = new PayPalClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        BrowserSwitchResult result = sut.deliverBrowserSwitchResultFromCache(activity);
        assertSame(browserSwitchResult, result);
    }
}