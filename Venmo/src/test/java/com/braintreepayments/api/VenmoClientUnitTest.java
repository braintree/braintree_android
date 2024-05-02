package com.braintreepayments.api;

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

import com.braintreepayments.api.core.Authorization;
import com.braintreepayments.api.core.BraintreeClient;
import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.BraintreeRequestCodes;
import com.braintreepayments.api.core.Configuration;

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

    private VenmoApi venmoApi;
    private Authorization clientToken;
    private Authorization tokenizationKey;
    private BrowserSwitchResultInfo browserSwitchResult;
    private VenmoPaymentAuthResult.Success paymentAuthResult;
    private final Uri SUCCESS_URL = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/success?resource_id=a-resource-id");
    private final Uri SUCCESS_URL_WITHOUT_RESOURCE_ID = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/success?username=venmojoe&payment_method_nonce=fakenonce");
    private final Uri CANCEL_URL = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/cancel");

    private final String LINK_TYPE = "universal";

    @Before
    public void beforeEach() throws JSONException {
        context = ApplicationProvider.getApplicationContext();
        venmoApi = mock(VenmoApi.class);

        venmoEnabledConfiguration =
                Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        venmoDisabledConfiguration =
                Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        venmoTokenizeCallback = mock(VenmoTokenizeCallback.class);
        venmoPaymentAuthRequestCallback = mock(VenmoPaymentAuthRequestCallback.class);
        sharedPrefsWriter = mock(VenmoSharedPrefsWriter.class);

        clientToken = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        tokenizationKey = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        browserSwitchResult = mock(BrowserSwitchResultInfo.class);
        VenmoPaymentAuthResultInfo venmoPaymentAuthResultInfo =
                new VenmoPaymentAuthResultInfo(browserSwitchResult);
        paymentAuthResult = new VenmoPaymentAuthResult.Success(venmoPaymentAuthResultInfo);
    }



    @Test
    public void createPaymentAuthRequest_whenCreatePaymentContextFails_collectAddressWithEcdDisabled() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setCollectCustomerBillingAddress(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(
                "Cannot collect customer data when ECD is disabled. Enable this feature in the Control Panel to collect this data.",
                ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
    }

    @Test
    public void createPaymentAuthRequest_whenCreatePaymentContextSucceeds_createsVenmoAuthChallenge() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("fake-session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .returnUrlScheme("com.example")
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("sample-venmo-merchant");
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        InOrder inOrder = Mockito.inOrder(venmoPaymentAuthRequestCallback, braintreeClient);
        inOrder.verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        inOrder.verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());

        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.ReadyToLaunch);
        VenmoPaymentAuthRequestParams params = ((VenmoPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest).getRequestParams();

        BrowserSwitchOptions browserSwitchOptions = params.getBrowserSwitchOptions();
        assertEquals(BraintreeRequestCodes.VENMO, browserSwitchOptions.getRequestCode());
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
        String expectedMetadata = String.format("{\"_meta\":{\"platform\":\"android\",\"sessionId\":\"fake-session-id\",\"integration\":\"custom\",\"version\":\"%s\"}}", com.braintreepayments.api.core.BuildConfig.VERSION_NAME);
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

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Configuration fetching error", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
    }

    @Test
    public void createPaymentAuthRequest_whenVenmoNotEnabled_forwardsExceptionToListener() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoDisabledConfiguration)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals("Venmo is not enabled", ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError().getMessage());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
    }

    @Test
    public void createPaymentAuthRequest_whenProfileIdIsNull_appSwitchesWithMerchantId() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
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
    public void createPaymentAuthRequest_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .integration("custom")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId("second-pwv-profile-id");
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
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

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED);
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(context, true);
    }

    @Test
    public void createPaymentAuthRequest_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        verify(sharedPrefsWriter).persistVenmoVaultOption(context, false);
    }

    @Test
    public void createPaymentAuthRequest_withTokenizationKey_persistsVenmoVaultFalse() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createPaymentContextSuccess("venmo-payment-context-id")
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(false);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
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

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback);

        ArgumentCaptor<VenmoPaymentAuthRequest> captor =
                ArgumentCaptor.forClass(VenmoPaymentAuthRequest.class);
        verify(venmoPaymentAuthRequestCallback).onVenmoPaymentAuthRequest(captor.capture());
        VenmoPaymentAuthRequest paymentAuthRequest = captor.getValue();
        assertTrue(paymentAuthRequest instanceof VenmoPaymentAuthRequest.Failure);
        assertEquals(graphQLError, ((VenmoPaymentAuthRequest.Failure) paymentAuthRequest).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_withPaymentContextId_requestFromVenmoApi() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("a-resource-id"),
                any(VenmoInternalCallback.class));

        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED);
    }

    @Test
    public void tokenize_withPaymentAuthResult_whenUserCanceled_returnsCancelAndSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(CANCEL_URL);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Cancel);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED, null, LINK_TYPE);

    }

    @Test
    public void tokenize_onGraphQLPostSuccess_returnsNonceToListener_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("venmojoe", nonce.getUsername());

        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_onGraphQLPostFailure_forwardsExceptionToListener_andSendsAnalytics() {
        BraintreeException graphQLError = new BraintreeException("graphQL error");
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTErrorResponse(graphQLError)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextError(graphQLError)
                .build();

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(graphQLError, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_withPaymentContext_performsVaultRequestIfRequestPersisted() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoAccountNonce nonce = mock(VenmoAccountNonce.class);
        when(nonce.getString()).thenReturn("some-nonce");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(nonce)
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("some-nonce"), any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_postsPaymentMethodNonceOnSuccess() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .authorizationSuccess(clientToken)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).createNonceFromPaymentContext(eq("a-resource-id"),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_performsVaultRequestIfRequestPersisted() throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .configuration(venmoEnabledConfiguration)
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
                .build();

        VenmoRequest request = new VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE);
        request.setProfileId(null);
        request.setShouldVault(true);

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi).vaultVenmoAccountNonce(eq("fake-venmo-nonce"),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_doesNotPerformRequestIfTokenizationKeyUsed() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("another-session-id")
                .authorizationSuccess(tokenizationKey)
                .build();
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        verify(venmoApi, never()).vaultVenmoAccountNonce(anyString(),
                any(VenmoInternalCallback.class));
    }

    @Test
    public void tokenize_withSuccessfulVaultCall_forwardsResultToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL_WITHOUT_RESOURCE_ID);

        VenmoAccountNonce venmoAccountNonce = mock(VenmoAccountNonce.class);

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
                .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Success);
        VenmoAccountNonce nonce = ((VenmoResult.Success) result).getNonce();
        assertEquals(venmoAccountNonce, nonce);
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_withFailedVaultCall_forwardsErrorToActivityResultListener_andSendsAnalytics() {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL_WITHOUT_RESOURCE_ID);

        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .vaultVenmoAccountNonceError(error)
                .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);
    }

    @Test
    public void tokenize_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics()
            throws JSONException {
        BraintreeClient braintreeClient = new MockBraintreeClientBuilder()
                .sessionId("session-id")
                .authorizationSuccess(clientToken)
                .sendGraphQLPOSTSuccessfulResponse(
                        Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
                .build();
        when(braintreeClient.getApplicationContext()).thenReturn(context);
        when(browserSwitchResult.getDeepLinkUrl()).thenReturn(SUCCESS_URL);

        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));
        Exception error = new Exception("error");

        VenmoApi venmoApi = new MockVenmoApiBuilder()
                .createNonceFromPaymentContextSuccess(venmoAccountNonce)
                .vaultVenmoAccountNonceError(error)
                .build();

        when(sharedPrefsWriter.getVenmoVaultOption(context)).thenReturn(true);

        VenmoClient sut =
                new VenmoClient(braintreeClient, venmoApi, sharedPrefsWriter);

        sut.tokenize(paymentAuthResult, venmoTokenizeCallback);

        ArgumentCaptor<VenmoResult> captor = ArgumentCaptor.forClass(VenmoResult.class);
        verify(venmoTokenizeCallback).onVenmoResult(captor.capture());

        VenmoResult result = captor.getValue();
        assertTrue(result instanceof VenmoResult.Failure);
        assertEquals(error, ((VenmoResult.Failure) result).getError());
        verify(braintreeClient).sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, null, LINK_TYPE);

    }
}