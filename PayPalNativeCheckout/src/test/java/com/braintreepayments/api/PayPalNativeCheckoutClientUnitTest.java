package com.braintreepayments.api;

import android.app.Application;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.cancel.OnCancel;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.error.OnError;
import com.paypal.checkout.shipping.OnShippingChange;

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

        when(activity.getApplication()).thenReturn(mock(Application.class));

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NATIVE);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
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
    public void requestOneTimePayment_startsNativeCheckout() {
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalCheckoutRequest.setReturnUrl("returnUrl://paypalpay");

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
        ArgumentCaptor<OnApprove> onApproveCaptor = ArgumentCaptor.forClass(OnApprove.class);
        ArgumentCaptor<OnShippingChange> onShippingChangeCaptor = ArgumentCaptor.forClass(OnShippingChange.class);
        ArgumentCaptor<OnCancel> onCancelCaptor = ArgumentCaptor.forClass(OnCancel.class);
        ArgumentCaptor<OnError> onErrorCaptor = ArgumentCaptor.forClass(OnError.class);

        PowerMockito.mockStatic(PayPalCheckout.class);
        PayPalCheckout.setConfig(
            new CheckoutConfig(
                activity.getApplication(),
                payPalEnabledConfig.getPayPalClientId(),
                Environment.SANDBOX,
                payPalCheckoutRequest.getReturnUrl()
            )
        );
        OnApprove onApprove = approval -> {
            PayPalNativeCheckoutAccount payPalAccount = new PayPalNativeCheckoutAccount();
            String paymentType = "single-payment";
            payPalAccount.setClientMetadataId(payPalEnabledConfig.getPayPalClientId());
            payPalAccount.setIntent(payPalResponse.getIntent());
            payPalAccount.setSource("paypal-browser");
            payPalAccount.setPaymentType(paymentType);
            payPalInternalClient.tokenize(payPalAccount, (payPalAccountNonce, error) -> {
                if (payPalAccountNonce != null) {
                    listener.onPayPalSuccess(payPalAccountNonce);
                } else {
                    listener.onPayPalFailure(new Exception("PaypalAccountNonce is null"));
                }
            });
        };
        OnCancel onCancel = () -> listener.onPayPalFailure(new Exception("User has canceled"));
        OnError onError = errorInfo -> listener.onPayPalFailure(new Exception(errorInfo.getError().getMessage()));

        PayPalCheckout.registerCallbacks(
            onApprove,
            null,
            onCancel,
            onError
        );

        PowerMockito.verifyStatic(PayPalCheckout.class);
        PayPalCheckout.setConfig(configCaptor.capture());

        PowerMockito.verifyStatic(PayPalCheckout.class);
        PayPalCheckout.registerCallbacks(onApproveCaptor.capture(), onShippingChangeCaptor.capture(), onCancelCaptor.capture(), onErrorCaptor.capture());

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(activity, lifecycle, braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.tokenizePayPalAccount(activity, payPalCheckoutRequest);

        assertEquals(payPalEnabledConfig.getPayPalClientId(), configCaptor.getValue().getClientId());
        assertEquals(onApprove, onApproveCaptor.getValue());
        assertEquals(onCancel, onCancelCaptor.getValue());
        assertEquals(onError, onErrorCaptor.getValue());
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
}