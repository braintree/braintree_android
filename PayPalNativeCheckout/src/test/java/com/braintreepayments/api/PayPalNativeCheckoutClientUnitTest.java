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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.config.CheckoutConfig;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PayPalCheckout.class })
public class PayPalNativeCheckoutClientUnitTest {

    private FragmentActivity activity;
    private Lifecycle lifecycle;
    private PayPalNativeCheckoutListener listener;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        lifecycle = mock(Lifecycle.class);
        listener = mock(PayPalNativeCheckoutListener.class);

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void constructor_setsLifecycleObserver() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        ArgumentCaptor<PayPalNativeLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalNativeLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalNativeLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withFragment_passesFragmentLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        Fragment fragment = mock(Fragment.class);
        when(fragment.getActivity()).thenReturn(activity);
        when(fragment.getLifecycle()).thenReturn(lifecycle);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(fragment, braintreeClient);

        ArgumentCaptor<PayPalNativeLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalNativeLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalNativeLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withFragmentActivity_passesActivityLifecycleAndActivityToObserver() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getLifecycle()).thenReturn(lifecycle);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, braintreeClient);

        ArgumentCaptor<PayPalNativeLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalNativeLifecycleObserver.class);
        verify(lifecycle).addObserver(captor.capture());

        PayPalNativeLifecycleObserver observer = captor.getValue();
        assertSame(sut, observer.payPalClient);
    }

    @Test
    public void constructor_withoutFragmentOrActivity_doesNotSetObserver() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(null, null, braintreeClient, payPalInternalClient);

        ArgumentCaptor<PayPalNativeLifecycleObserver> captor = ArgumentCaptor.forClass(PayPalNativeLifecycleObserver.class);
        verify(lifecycle, never()).addObserver(captor.capture());
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultExists_deliversResultToListener_andSetsPendingResultNull() throws JSONException {
        PayPalNativeCheckoutAccountNonce nonce = mock(PayPalNativeCheckoutAccountNonce.class);
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
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

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.pendingBrowserSwitchResult = browserSwitchResult;
        sut.setListener(listener);

        verify(listener).onPayPalSuccess(same(nonce));
        verify(listener, never()).onPayPalFailure(any(Exception.class));
        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void setListener_whenPendingBrowserSwitchResultDoesNotExist_doesNotInvokeListener() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.pendingBrowserSwitchResult = null;
        sut.setListener(listener);

        verify(listener, never()).onPayPalSuccess(any(PayPalNativeCheckoutAccountNonce.class));
        verify(listener, never()).onPayPalFailure(any(Exception.class));

        assertNull(sut.pendingBrowserSwitchResult);
    }

    @Test
    public void tokenizePayPalAccount_whenPayPalNotEnabled_returnsErrorToListener() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalNativeCheckoutVaultRequest());

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_whenDeviceCantPerformBrowserSwitch_returnsErrorToListener() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalNativeCheckoutVaultRequest());

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developers.braintreepayments.com/guides/client-sdk/android/#browser-switch " +
                "for the correct configuration", errorCaptor.getValue().getMessage());
    }

    @Test
    public void tokenizePayPalAccount_sendsAnalyticsEvents() {
        PayPalNativeCheckoutVaultRequest payPalVaultRequest = new PayPalNativeCheckoutVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalVaultRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(true)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, payPalVaultRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.app-switch.started");
    }

    @Test
    public void requestOneTimePayment_startsNativeCheckout() throws JSONException, BrowserSwitchException {
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalCheckoutRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        ArgumentCaptor<CheckoutConfig> configCaptor = ArgumentCaptor.forClass(CheckoutConfig.class);

        PayPalCheckout mockCheckout = mock(PayPalCheckout.class);
        PowerMockito.mockStatic(PayPalCheckout.class);
        PowerMockito.when(PayPalCheckout.INSTANCE).thenReturn(mockCheckout);
        PayPalCheckout myName = PowerMockito.mock(PayPalCheckout.class);
        PowerMockito.doNothing().when(myName);
        PayPalCheckout.setConfig(configCaptor.capture());

        configCaptor.getValue();
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
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
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalDisabledConfig)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalNativeCheckoutRequest("1.00"));

        ArgumentCaptor<Exception> errorCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof BraintreeException);
        assertEquals("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.", errorCaptor.getValue().getMessage());
    }

    @Test
    public void requestOneTimePayment_whenDeviceCantPerformBrowserSwitch_returnsErrorToListener() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(false)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, new PayPalNativeCheckoutRequest("1.00"));

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
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalCheckoutRequest)
                .approvalUrl("https://example.com/approval/url")
                .successUrl("https://example.com/success/url")
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, payPalCheckoutRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.app-switch.started");
    }

    @Test
    public void requestOneTimePayment_sendsPayPalPayLaterOfferedAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.00");
        request.setShouldOfferPayLater(true);
        sut.tokenizePayPalAccount(activity, request);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.paylater.offered");
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalRequestViaInternalClient() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalNativeCheckoutInternalClientCallback.class));
    }

    @Test
    public void requestOneTimePayment_sendsPayPalRequestViaInternalClient() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalNativeCheckoutInternalClientCallback.class));
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalCreditOfferedAnalyticsEvent() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setShouldOfferCredit(true);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.credit.offered");
    }

    @Test
    public void onBrowserSwitchResult_withBillingAgreement_tokenizesResponseOnSuccess() throws JSONException {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<PayPalNativeCheckoutAccount> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(), any(PayPalNativeCheckoutBrowserSwitchResultCallback.class));

        PayPalNativeCheckoutAccount payPalAccount = captor.getValue();
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
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<PayPalNativeCheckoutAccount> captor = ArgumentCaptor.forClass(PayPalNativeCheckoutAccount.class);
        verify(payPalInternalClient).tokenize(captor.capture(), any(PayPalNativeCheckoutBrowserSwitchResultCallback.class));

        PayPalNativeCheckoutAccount payPalAccount = captor.getValue();
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
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(braintreeClient).sendAnalyticsEvent("paypal.billing-agreement.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_oneTimePayment_sendsAnalyticsEvents() throws JSONException {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(braintreeClient).sendAnalyticsEvent("paypal.single-payment.browser-switch.succeeded");
    }

    @Test
    public void onBrowserSwitchResult_whenPayPalCreditPresent_sendsAnalyticsEvents() throws JSONException {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .tokenizeSuccess(PayPalNativeCheckoutAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE)))
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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        verify(braintreeClient).sendAnalyticsEvent("paypal.credit.accepted");
    }

    @Test
    public void onBrowserSwitchResult_whenCancelUriReceived_notifiesCancellationAndSendsAnalyticsEvent() throws JSONException {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void onBrowserSwitchResult_whenBrowserSwitchCanceled_forwardsExceptionAndSendsAnalyticsEvent() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getStatus()).thenReturn(BrowserSwitchStatus.CANCELED);

        when(browserSwitchResult.getRequestMetadata()).thenReturn(new JSONObject());
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(captor.capture());

        Exception exception = captor.getValue();
        assertTrue(exception instanceof UserCanceledException);
        assertEquals("User canceled PayPal.", exception.getMessage());

        verify(braintreeClient).sendAnalyticsEvent(eq("paypal.single-payment.browser-switch.canceled"));
    }

    @Test
    public void onBrowserSwitchResult_whenPayPalInternalClientTokenizeResult_forwardsResultToListener() throws JSONException {
        PayPalNativeCheckoutAccountNonce payPalAccountNonce = mock(PayPalNativeCheckoutAccountNonce.class);
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .deliverBrowserSwitchResult(browserSwitchResult)
                .build();
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.onBrowserSwitchResult(activity);
        verify(listener).onPayPalSuccess(same(payPalAccountNonce));
    }

    @Test
    public void getBrowserSwitchResult_getsBrowserSwitchResultFromBraintreeClient() {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        BraintreeClient braintreeClient = mock(BraintreeClient.class);
        when(braintreeClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);

        BrowserSwitchResult result = sut.getBrowserSwitchResult(activity);
        assertSame(browserSwitchResult, result);
    }
}