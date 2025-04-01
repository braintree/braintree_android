package com.braintreepayments.api.paypal;

import static com.braintreepayments.api.paypal.PayPalClient.BROWSER_SWITCH_EXCEPTION_MESSAGE;
import static com.braintreepayments.api.paypal.PayPalClient.PAYPAL_NOT_ENABLED_MESSAGE;
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
import com.braintreepayments.api.core.AnalyticsParamRepository;
import com.braintreepayments.api.core.AppSwitchRepository;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.GetAppSwitchUseCase;
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase;
import com.braintreepayments.api.core.GetReturnLinkUseCase;
import com.braintreepayments.api.core.LinkType;
import com.braintreepayments.api.core.MerchantRepository;
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

    private MerchantRepository merchantRepository;
    private GetReturnLinkTypeUseCase getReturnLinkTypeUseCase;
    private GetReturnLinkUseCase getReturnLinkUseCase;
    private AppSwitchRepository appSwitchRepository;
    private GetAppSwitchUseCase getAppSwitchUseCase;
    private AnalyticsParamRepository analyticsParamRepository;

    @Before
    public void beforeEach() throws JSONException {
        activity = mock(FragmentActivity.class);
        merchantRepository = mock(MerchantRepository.class);
        payPalEnabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        payPalDisabledConfig = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL);

        payPalTokenizeCallback = mock(PayPalTokenizeCallback.class);
        paymentAuthCallback = mock(PayPalPaymentAuthCallback.class);

        merchantRepository = mock(MerchantRepository.class);
        getReturnLinkTypeUseCase = mock(GetReturnLinkTypeUseCase.class);
        getReturnLinkUseCase = mock(GetReturnLinkUseCase.class);
        getAppSwitchUseCase = mock(GetAppSwitchUseCase.class);
        analyticsParamRepository = mock(AnalyticsParamRepository.class);

        when(merchantRepository.getReturnUrlScheme()).thenReturn("com.braintreepayments.demo");
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.AppLink(
            Uri.parse("www.example.com")
        ));

        when(getAppSwitchUseCase.invoke()).thenReturn(true);

        when(getReturnLinkTypeUseCase.invoke()).thenReturn(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK);
    }

    @Test
    public void initialization_sets_app_link_in_analyticsParamRepository() {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        );

        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
            .sendRequestSuccess(paymentAuthRequest)
            .build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        new PayPalClient(
            braintreeClient,
            payPalInternalClient,
            merchantRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase,
            getAppSwitchUseCase,
            analyticsParamRepository
        );

        verify(analyticsParamRepository).setLinkType(LinkType.APP_LINK);
    }

    @Test
    public void initialization_sets_deep_link_in_analyticsParamRepository() {
        when(getReturnLinkTypeUseCase.invoke()).thenReturn(GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK);
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        PayPalPaymentAuthRequestParams paymentAuthRequest = new PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        );

        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder()
            .sendRequestSuccess(paymentAuthRequest)
            .build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        new PayPalClient(
            braintreeClient,
            payPalInternalClient,
            merchantRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase,
            getAppSwitchUseCase,
            analyticsParamRepository
        );

        verify(analyticsParamRepository).setLinkType(LinkType.DEEP_LINK);
    }

    @Test
    public void createPaymentAuthRequest_callsBackPayPalResponse_sendsStartedAnalytics() throws JSONException {
        PayPalVaultRequest payPalVaultRequest = new PayPalVaultRequest(true);
        payPalVaultRequest.setMerchantAccountId("sample-merchant-account-id");
        payPalVaultRequest.setShopperSessionId("test-shopper-session-id");

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

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
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
            new AnalyticsEventParams(null, true, null, null, null, null, null, "test-shopper-session-id"),
            true
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

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
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

        when(merchantRepository.getAppLinkReturnUri()).thenReturn(Uri.parse("www.example.com"));

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().configuration(payPalEnabledConfig)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        assertEquals(merchantRepository.getAppLinkReturnUri(),
            ((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams().getBrowserSwitchOptions().getAppLinkUri());
    }

    @Test
    public void createPaymentAuthRequest_setsDeepLinkReturnUrlScheme() {
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.DeepLink(
            "com.braintreepayments.demo"
        ));
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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().configuration(payPalEnabledConfig)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.ReadyToLaunch);
        assertEquals("com.braintreepayments.demo",
            ((PayPalPaymentAuthRequest.ReadyToLaunch) request).getRequestParams().getBrowserSwitchOptions().getReturnUrlScheme());
    }

    @Test
    public void createPaymentAuthRequest_returnsAnErrorWhen_getReturnLinkUseCase_returnsAFailure() {
        BraintreeException exception = new BraintreeException();
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.Failure(exception));

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

        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().configuration(payPalEnabledConfig)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor = ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(exception, ((PayPalPaymentAuthRequest.Failure) request).getError());
    }

    @Test
    public void createPaymentAuthRequest_whenPayPalNotEnabled_returnsError() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalDisabledConfig).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00", true),
            paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(PAYPAL_NOT_ENABLED_MESSAGE, ((PayPalPaymentAuthRequest.Failure) request).getError().getMessage());

        AnalyticsEventParams params = new AnalyticsEventParams(
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PAYPAL_NOT_ENABLED_MESSAGE
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true);
        verify(analyticsParamRepository).reset();
    }

    @Test
    public void createPaymentAuthRequest_whenCheckoutRequest_whenConfigError_forwardsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String errorMessage = "Error fetching auth";
        Exception authError = new Exception(errorMessage);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(authError)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, new PayPalCheckoutRequest("1.00", true), paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(authError, ((PayPalPaymentAuthRequest.Failure) request).getError());

        AnalyticsEventParams params = new AnalyticsEventParams(
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            errorMessage
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true);
    }

    @Test
    public void requestBillingAgreement_whenConfigError_forwardsErrorToListener() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        String errorMessage = "Error fetching auth";
        Exception authError = new Exception(errorMessage);
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(authError)
            .build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, new PayPalVaultRequest(true), paymentAuthCallback);

        ArgumentCaptor<PayPalPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(PayPalPaymentAuthRequest.class);
        verify(paymentAuthCallback).onPayPalPaymentAuthRequest(captor.capture());

        PayPalPaymentAuthRequest request = captor.getValue();
        assertTrue(request instanceof PayPalPaymentAuthRequest.Failure);
        assertEquals(authError, ((PayPalPaymentAuthRequest.Failure) request).getError());

        AnalyticsEventParams params = new AnalyticsEventParams(
            null,
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            errorMessage
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true);
    }

    @Test
    public void createPaymentAuthRequest_sets_analyticsParamRepository_merchantEnabledAppSwitch() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(
            true,
            false,
            null,
            null,
            true
        );

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback);

        verify(analyticsParamRepository).setMerchantEnabledAppSwitch(true);
    }

    @Test
    public void createPaymentAuthRequest_whenVaultRequest_sendsPayPalRequestViaInternalClient() {
        PayPalInternalClient payPalInternalClient = new MockPayPalInternalClientBuilder().build();

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalVaultRequest payPalRequest = new PayPalVaultRequest(true);

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
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

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
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

        when(payPalInternalClient.isPayPalInstalled(activity)).thenReturn(true);
        when(payPalInternalClient.isAppSwitchEnabled(payPalVaultRequest)).thenReturn(true);

        BraintreeClient braintreeClient =
            new MockBraintreeClientBuilder().configuration(payPalEnabledConfig).build();

        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);
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


        AnalyticsEventParams params = new AnalyticsEventParams(
            null,
            true
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_STARTED, params, true);
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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Cancel);

        AnalyticsEventParams params = new AnalyticsEventParams(
            null,
            false
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true);
        verify(analyticsParamRepository).reset();
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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Success);
        assertEquals(payPalAccountNonce, ((PayPalResult.Success) result).getNonce());

        AnalyticsEventParams params = new AnalyticsEventParams(
            "EC-HERMES-SANDBOX-EC-TOKEN",
            false
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params, true);
        verify(analyticsParamRepository).reset();
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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Success);
        assertEquals(payPalAccountNonce, ((PayPalResult.Success) result).getNonce());

        AnalyticsEventParams params = new AnalyticsEventParams(
            "EC-HERMES-SANDBOX-EC-TOKEN"
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params, true);
        AnalyticsEventParams appSwitchParams = new AnalyticsEventParams(
            "EC-HERMES-SANDBOX-EC-TOKEN",
            false,
            null,
            null,
            null,
            null,
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN&switch_initiated_time=17166111926211"
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_SUCCEEDED, appSwitchParams, true);
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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        AnalyticsEventParams params = new AnalyticsEventParams(
            "SOME-BA",
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BROWSER_SWITCH_EXCEPTION_MESSAGE
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true);
        AnalyticsEventParams appSwitchParams = new AnalyticsEventParams(
            "SOME-BA",
            false,
            null,
            null,
            null,
            null,
            "https://some-scheme/onetouch/v1/cancel?token=SOME-BA&switch_initiated_time=17166111926211",
            null,
            null,
            null,
            null,
            BROWSER_SWITCH_EXCEPTION_MESSAGE
        );
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_FAILED, appSwitchParams, true);
        verify(analyticsParamRepository).reset();
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
        PayPalClient sut = new PayPalClient(braintreeClient, payPalInternalClient, merchantRepository, getReturnLinkTypeUseCase, getReturnLinkUseCase, getAppSwitchUseCase, analyticsParamRepository);

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback);

        ArgumentCaptor<PayPalResult> captor = ArgumentCaptor.forClass(PayPalResult.class);
        verify(payPalTokenizeCallback).onPayPalResult(captor.capture());

        PayPalResult result = captor.getValue();
        assertTrue(result instanceof PayPalResult.Cancel);

        AnalyticsEventParams params = new AnalyticsEventParams();
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true);
        verify(braintreeClient).sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, params, true);
    }
}