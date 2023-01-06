package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.fragment.app.FragmentActivity;

import com.paypal.checkout.PayPalCheckout;
import com.paypal.checkout.approve.ApprovalData;
import com.paypal.checkout.approve.OnApprove;
import com.paypal.checkout.cancel.OnCancel;
import com.paypal.checkout.config.CheckoutConfig;
import com.paypal.checkout.config.Environment;
import com.paypal.checkout.error.OnError;
import com.paypal.checkout.shipping.OnShippingChange;
import com.paypal.pyplcheckout.common.instrumentation.PEnums;
import com.paypal.pyplcheckout.common.instrumentation.PLog;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.powermock.*", "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
@PrepareForTest({PayPalCheckout.class, PLog.class})
public class PayPalNativeCheckoutClientUnitTest {

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private FragmentActivity activity;
    private PayPalNativeCheckoutListener listener;

    private Configuration payPalEnabledConfig;
    private Configuration payPalDisabledConfig;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        listener = mock(PayPalNativeCheckoutListener.class);

        when(activity.getApplication()).thenReturn(mock(Application.class));

        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL_NATIVE);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);
    }

    @Test
    public void tokenizePayPalAccount_throwsWhenPayPalRequestIsBaseClass() {
        PayPalNativeRequest baseRequest = new PayPalNativeRequest() {
            @Override
            String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
                return null;
            }
        };
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);

        Exception capturedException = null;
        try {
            sut.tokenizePayPalAccount(activity, baseRequest);
        } catch (Exception e) {
            capturedException = e;
        }
        assertNotNull(capturedException);
        String expectedMessage = "Unsupported request type. Please use either a "
                + "PayPalNativeCheckoutRequest or a PayPalNativeCheckoutVaultRequest.";
        assertEquals(expectedMessage, capturedException.getMessage());
    }

    @Test
    public void requestBillingAgreement_launchNativeCheckout_sendsAnalyticsEvents() {
        PayPalNativeCheckoutVaultRequest payPalVaultRequest = new PayPalNativeCheckoutVaultRequest();
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalVaultRequest.setReturnUrl("returnUrl://paypalpay");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalVaultRequest)
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .canPerformBrowserSwitch(true)
                .build();
        PowerMockito.mockStatic(PayPalCheckout.class);
        PowerMockito.mockStatic(PLog.class);
        PLog.transition(
            PEnums.TransitionName.BRAINTREE_ROUTING,
            PEnums.Outcome.THIRD_PARTY,
            PEnums.EventCode.E233,
            PEnums.StateName.BRAINTREE,
            null,
            null,
            null,
            null,
            null,
            null,
            "BrainTree"
        );
        PowerMockito.verifyStatic(PLog.class);
        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.launchNativeCheckout(activity, payPalVaultRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.started");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.succeeded");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.billing-agreement.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.billing-agreement.started");
    }

    @Test
    public void requestOneTimePayment_launchNativeCheckout_sendsAnalyticsEvents() {
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalCheckoutRequest.setReturnUrl("returnUrl://paypalpay");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalCheckoutRequest)
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
                Environment.SANDBOX
            )
        );
        OnApprove onApprove = approval -> { };
        OnCancel onCancel = () -> { };
        OnError onError = errorInfo -> { };

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

        PowerMockito.mockStatic(PLog.class);
        PLog.transition(
                PEnums.TransitionName.BRAINTREE_ROUTING,
                PEnums.Outcome.THIRD_PARTY,
                PEnums.EventCode.E233,
                PEnums.StateName.BRAINTREE,
                null,
                null,
                null,
                null,
                null,
                null,
                "BrainTree"
        );
        PowerMockito.verifyStatic(PLog.class);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.launchNativeCheckout(activity, payPalCheckoutRequest);

        assertEquals(payPalEnabledConfig.getPayPalClientId(), configCaptor.getValue().getClientId());
        assertEquals(onApprove, onApproveCaptor.getValue());
        assertEquals(onCancel, onCancelCaptor.getValue());
        assertEquals(onError, onErrorCaptor.getValue());

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.started");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.succeeded");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.single-payment.started");
    }

    @Test
    public void paypalAccount_isSetupCorrectly() throws JSONException {
        String riskCorrelationId = "riskId";
        String sampleMerchantId = "sample-merchant-account-id";
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId(sampleMerchantId);
        payPalCheckoutRequest.setReturnUrl("returnUrl://paypalpay");
        payPalCheckoutRequest.setRiskCorrelationId(riskCorrelationId);

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalCheckoutRequest)
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);

        ApprovalData approvalData = new ApprovalData(null, null, null, null, null, null, null, null, null);
        PayPalNativeCheckoutAccount account = sut.setupAccount(payPalCheckoutRequest, approvalData);

        assertEquals(account.getClientMetadataId(), riskCorrelationId);
        assertEquals(account.getMerchantAccountId(), sampleMerchantId);
    }

    @Test
    public void requestOneTimePayment_sendsBrowserSwitchStartAnalyticsEvent() throws Exception {
        PayPalNativeCheckoutRequest payPalCheckoutRequest = new PayPalNativeCheckoutRequest("1.00");
        payPalCheckoutRequest.setIntent("authorize");
        payPalCheckoutRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalCheckoutRequest.setReturnUrl("returnUrl://paypalpay");

        PayPalNativeCheckoutResponse payPalResponse = new PayPalNativeCheckoutResponse(payPalCheckoutRequest)
                .clientMetadataId("sample-client-metadata-id");
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
                .sendRequestSuccess(payPalResponse)
                .build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();
        PowerMockito.mockStatic(PayPalCheckout.class);

        PowerMockito.mockStatic(PLog.class);
        PLog.transition(
                PEnums.TransitionName.BRAINTREE_ROUTING,
                PEnums.Outcome.THIRD_PARTY,
                PEnums.EventCode.E233,
                PEnums.StateName.BRAINTREE,
                null,
                null,
                null,
                null,
                null,
                null,
                "BrainTree"
        );
        PowerMockito.verifyStatic(PLog.class);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.setListener(listener);
        sut.launchNativeCheckout(activity, payPalCheckoutRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.single-payment.selected");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.single-payment.started");
    }

    @Test
    public void requestOneTimePayment_sendsPayPalPayLaterOfferedAnalyticsEvent() throws Exception {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        PayPalNativeCheckoutRequest request = new PayPalNativeCheckoutRequest("1.00");
        request.setShouldOfferPayLater(true);
        sut.tokenizePayPalAccount(activity, request);

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.single-payment.paylater.offered");
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalRequestViaInternalClient() throws Exception {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback.class));
    }

    @Test
    public void requestOneTimePayment_sendsPayPalRequestViaInternalClient() throws Exception {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(payPalEnabledConfig)
                .build();

        PayPalNativeCheckoutRequest payPalRequest = new PayPalNativeCheckoutRequest("1.00");

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(payPalInternalClient).sendRequest(same(activity), same(payPalRequest), any(PayPalNativeCheckoutInternalClient.PayPalNativeCheckoutInternalClientCallback.class));
    }

    @Test
    public void tokenizePayPalAccount_sendsPayPalCreditOfferedAnalyticsEvent() throws Exception {
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();

        PayPalNativeCheckoutVaultRequest payPalRequest = new PayPalNativeCheckoutVaultRequest();
        payPalRequest.setShouldOfferCredit(true);

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.tokenizePayPalAccount(activity, payPalRequest);

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.billing-agreement.credit.offered");
    }

    @Test
    public void launchNativeCheckout_notifiesErrorWhenPayPalRequestIsBaseClass_sendsAnalyticsEvents() {
        PayPalNativeRequest baseRequest = new PayPalNativeRequest() {
            @Override
            String createRequestBody(Configuration configuration, Authorization authorization, String successUrl, String cancelUrl) throws JSONException {
                return null;
            }
        };
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        PayPalNativeCheckoutInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        PayPalNativeCheckoutClient sut = new PayPalNativeCheckoutClient(braintreeClient, payPalInternalClient);
        sut.setListener(listener);

        sut.launchNativeCheckout(activity, baseRequest);

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(listener).onPayPalFailure(captor.capture());

        Exception capturedException = captor.getValue();
        String expectedMessage = "Unsupported request type. Please use either a "
                + "PayPalNativeCheckoutRequest or a PayPalNativeCheckoutVaultRequest.";
        assertEquals(expectedMessage, capturedException.getMessage());

        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.started");
        verify(braintreeClient).sendAnalyticsEvent("paypal-native.tokenize.invalid-request.failed");
    }
}