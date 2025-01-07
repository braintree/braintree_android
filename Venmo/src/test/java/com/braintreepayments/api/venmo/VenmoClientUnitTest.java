package com.braintreepayments.api.venmo;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.BrowserSwitchFinalResult;
import com.braintreepayments.api.BrowserSwitchOptions;
import com.braintreepayments.api.core.AnalyticsEventParams;
import com.braintreepayments.api.core.AnalyticsParamRepository;
import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;
import com.braintreepayments.api.core.GetReturnLinkUseCase;
import com.braintreepayments.api.core.IntegrationType;
import com.braintreepayments.api.core.MerchantRepository;
import com.braintreepayments.api.testutils.Fixtures;
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VenmoClientUnitTest {

    private Context context;
    private Configuration venmoEnabledConfiguration;
    private Configuration venmoDisabledConfiguration;
    private VenmoTokenizeCallback venmoTokenizeCallback;
    private VenmoPaymentAuthRequestCallback venmoPaymentAuthRequestCallback;
    private VenmoSharedPrefsWriter sharedPrefsWriter;
    private AnalyticsParamRepository analyticsParamRepository;

    private VenmoApi venmoApi;
    private ApiClient apiClient;
    private Authorization clientToken;
    private Authorization tokenizationKey;
    private BrowserSwitchFinalResult.Success browserSwitchResult;
    private VenmoPaymentAuthResult.Success paymentAuthResult;
    private final Uri SUCCESS_URL = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/success?resource_id=a-resource-id");
    private final Uri SUCCESS_URL_WITHOUT_RESOURCE_ID = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/success?username=venmojoe&payment_method_nonce=fakenonce");
    private final Uri CANCEL_URL = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/cancel");

    private final String LINK_TYPE = "universal";
    private final Uri appSwitchUrl = Uri.parse("https://example.com");
    private final AnalyticsEventParams expectedAnalyticsParams = new AnalyticsEventParams(
        null,
        LINK_TYPE,
        false,
        null,
        null,
        null,
        null,
        appSwitchUrl.toString()
    );
    private final AnalyticsEventParams expectedVaultAnalyticsParams = new AnalyticsEventParams(
        null,
        LINK_TYPE,
        true,
        null,
        null,
        null,
        null,
        appSwitchUrl.toString()
    );

    private final MerchantRepository merchantRepository = mock(MerchantRepository.class);
    private final VenmoRepository venmoRepository = mock(VenmoRepository.class);
    private final GetReturnLinkUseCase getReturnLinkUseCase = mock(GetReturnLinkUseCase.class);

    @Before
    public void beforeEach() throws JSONException {
        context = ApplicationProvider.getApplicationContext();
        venmoApi = mock(VenmoApi.class);
        apiClient = mock(ApiClient.class);
        analyticsParamRepository = mock(AnalyticsParamRepository.class);

        venmoEnabledConfiguration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        venmoDisabledConfiguration =
            Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        venmoTokenizeCallback = mock(VenmoTokenizeCallback.class);
        venmoPaymentAuthRequestCallback = mock(VenmoPaymentAuthRequestCallback.class);
        sharedPrefsWriter = mock(VenmoSharedPrefsWriter.class);

        clientToken = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        tokenizationKey = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        browserSwitchResult = mock(BrowserSwitchFinalResult.Success.class);
        paymentAuthResult = new VenmoPaymentAuthResult.Success(browserSwitchResult);

        when(analyticsParamRepository.getSessionId()).thenReturn("session-id");
        when(merchantRepository.getIntegrationType()).thenReturn(IntegrationType.CUSTOM);
        when(merchantRepository.getApplicationContext()).thenReturn(context);
        when(venmoRepository.getVenmoUrl()).thenReturn(appSwitchUrl);
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.AppLink(appSwitchUrl));
    }

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentContextFails_collectAddressWithEcdDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setCollectCustomerBillingAddress(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedAnalyticsParams);
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(
            "Cannot collect customer data when ECD is disabled. Enable this feature in the Control Panel to collect this data.",
            ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
    }

    @Test
    public void createPaymentAuthRequest_withDeepLink_whenCreatePaymentContextSucceeds_createsVenmoAuthChallenge() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .returnUrlScheme("com.example")
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.DeepLink("com.example"));

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        InOrder inOrder = Mockito.inOrder(venmoPaymentAuthRequestCallback, braintreeClient);
        inOrder.verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED, new AnalyticsEventParams());

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        inOrder.verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());

        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();

        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.VENMO.getCode(), browserSwitchOptions.getRequestCode());
        assertEquals("com.example", browserSwitchOptions.getReturnUrlScheme());

        Uri url = browserSwitchOptions.getUrl();
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/success", url.getQueryParameter("x-success"));
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/error", url.getQueryParameter("x-error"));
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/cancel", url.getQueryParameter("x-cancel"));
        assertEquals("sample-venmo-merchant", url.getQueryParameter("braintree_merchant_id"));
        assertEquals("venmo-payment-context-id", url.getQueryParameter("resource_id"));
        assertEquals("MOBILE_APP", url.getQueryParameter("customerClient"));

        String metadata = url.getQueryParameter("braintree_sdk_data");
        String metadataString = new String(Base64.decode(metadata, Base64.DEFAULT));
        String expectedMetadata = String.format("{\"_meta\":{\"platform\":\"android\",\"sessionId\":\"session-id\",\"integration\":\"custom\",\"version\":\"%s\"}}", com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
        assertEquals(expectedMetadata, metadataString);
    }

    @Test
    public void createPaymentAuthRequest_whenConfigurationException_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configurationError(new Exception("Configuration fetching error"))
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Configuration fetching error", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedAnalyticsParams);
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoNotEnabled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoDisabledConfiguration)
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Venmo is not enabled", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedAnalyticsParams);
    }

    @Test
    public void createPaymentAuthRequest_whenProfileIdIsNull_appSwitchesWithMerchantId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();

        Uri url = browserSwitchOptions.getUrl();
        assertEquals("merchant-id", url.getQueryParameter("braintree_merchant_id"));
    }

    @Test
    public void createPaymentAuthRequest_whenAppLinkUriSet_appSwitchesWithAppLink() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();

        Uri url = browserSwitchOptions.getUrl();
        assertEquals("https://example.com/success", url.getQueryParameter("x-success"));
        assertEquals("https://example.com/error", url.getQueryParameter("x-error"));
        assertEquals("https://example.com/cancel", url.getQueryParameter("x-cancel"));
    }

    @Test
    public void createPaymentAuthRequest_throws_error_when_getReturnLinkUseCase_returnsFailure() {
        BraintreeException exception = new BraintreeException();
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);
        when(getReturnLinkUseCase.invoke()).thenReturn(new GetReturnLinkUseCase.ReturnLinkResult.Failure(exception));

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(exception, ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
    }

    @Test
    public void createPaymentAuthRequest_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();
        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();

        Uri url = browserSwitchOptions.getUrl();
        assertEquals("second-pwv-profile-id", url.getQueryParameter("braintree_merchant_id"));
        assertEquals("venmo-payment-context-id", url.getQueryParameter("resource_id"));

    }

    @Test
    public void createPaymentAuthRequest_sendsAnalyticsEvent() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED, new AnalyticsEventParams());
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(context, true);
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(context, false);
    }

    @Test
    public void createPaymentAuthRequest_withTokenizationKey_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();

        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(context, false);
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoApiError_forwardsErrorToListener_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("GraphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .sendGraphQLPOSTErrorResponse(graphQLError)
            .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createPaymentContextError(graphQLError)
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setShouldVault(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
            ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(graphQLError, ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedAnalyticsParams);
    }

    @Test
    public void tokenize_withPaymentContextId_requestFromVenmoApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("a-resource-id"),
            any(VenmoInternalCallback.class));

        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED, expectedAnalyticsParams);
    }

    @Test
    public void tokenize_withPaymentAuthResult_whenUserCanceled_returnsCancelAndSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(CANCEL_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Cancel);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED, expectedAnalyticsParams);

    }

    @Test
    public void tokenize_onGraphQLPostSuccess_returnsNonceToListener_andSendsAnalytics()
        throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
            .build();

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("venmojoe", nonce.getUsername());

        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, expectedAnalyticsParams);
    }

    @Test
    public void tokenize_onGraphQLPostFailure_forwardsExceptionToListener_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("graphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .sendGraphQLPOSTErrorResponse(graphQLError)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextError(graphQLError)
            .build();

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(graphQLError, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedAnalyticsParams);
    }

    @Test
    public void tokenize_withPaymentContext_performsVaultRequestIfRequestPersisted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoAccountNonce nonce = mock(VenmoAccountNonce.class);
        when(nonce.getString()).thenReturn("some-nonce");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(nonce)
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("some-nonce"), any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_postsPaymentMethodNonceOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("a-resource-id"),
            any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_performsVaultRequestIfRequestPersisted() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .configuration(venmoEnabledConfiguration)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
            .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("fake-venmo-nonce"),
            any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_doesNotPerformRequestIfTokenizationKeyUsed() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);
        when(merchantRepository.getAuthorization()).thenReturn(tokenizationKey);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi, never()).vaultVenmoAccountNonce(anyString(),
            any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_withSuccessfulVaultCall_forwardsResultToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL_WITHOUT_RESOURCE_ID);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoAccountNonce venmoAccountNonce = mock(VenmoAccountNonce.class);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
            .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, expectedVaultAnalyticsParams);
    }

    @Test
    public void tokenize_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics()
        throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
            new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(venmoAccountNonce)
            .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
            .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, expectedVaultAnalyticsParams);
    }

    @Test
    public void tokenize_withFailedVaultCall_forwardsErrorToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder().build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL_WITHOUT_RESOURCE_ID);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .vaultVenmoAccountNonceError(error)
            .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());

        AnalyticsEventParams params = new AnalyticsEventParams();
        params.setLinkType(LINK_TYPE);
        params.setVaultRequest(true);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedVaultAnalyticsParams);
    }

    @Test
    public void tokenize_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics()
        throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
            .build();
        when(browserSwitchResult.getReturnUrl()).thenReturn(SUCCESS_URL);
        when(merchantRepository.getAuthorization()).thenReturn(clientToken);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
            new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));
        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(venmoAccountNonce)
            .vaultVenmoAccountNonceError(error)
            .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut = new VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkUseCase
        );

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, expectedVaultAnalyticsParams);
    }
}